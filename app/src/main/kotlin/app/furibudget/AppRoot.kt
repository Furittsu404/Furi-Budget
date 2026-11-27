package app.furibudget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Group
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.furibudget.ui.activity.ActivityScreen
import app.furibudget.ui.appearance.AppearanceScreen
import app.furibudget.ui.components.FuriNavBar
import app.furibudget.ui.components.MoreEntry
import app.furibudget.ui.components.MoreMenu
import app.furibudget.ui.components.NavItem
import app.furibudget.ui.edit.EditScreen
import app.furibudget.ui.bills.BillEditScreen
import app.furibudget.ui.bills.BillsScreen
import app.furibudget.ui.bills.PaySheet
import app.furibudget.ui.budgets.BudgetsScreen
import app.furibudget.ui.home.HomeScreen
import app.furibudget.ui.onboarding.OnboardingScreen
import app.furibudget.ui.feedback.SoundSettingsScreen
import app.furibudget.ui.funds.GoalsScreen
import app.furibudget.ui.people.PeopleScreen
import app.furibudget.ui.rates.ExchangeRatesScreen
import app.furibudget.ui.people.PersonScreen
import app.furibudget.ui.settings.ManageScreen
import app.furibudget.ui.settings.SettingsScreen
import app.furibudget.ui.trash.TrashScreen
import app.furibudget.ui.insights.InsightsScreen
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriBackground
import app.furibudget.ui.theme.ThemeSpec
import kotlinx.coroutines.launch

private object Route {
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val INSIGHTS = "insights"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val TRASH = "trash"
    const val PEOPLE = "people"
    const val RATES = "rates"
    const val FEEDBACK = "feedback"
    const val PERSON = "person/{id}"
    const val BILLS = "bills"
    const val BILL_EDIT = "bill/{id}"
    const val APPEARANCE = "appearance"
    const val SETTINGS = "settings"
    const val MANAGE = "manage"
    const val EDIT = "edit/{id}?kind={kind}"
    fun edit(id: Long, kind: app.furibudget.data.TxKind = app.furibudget.data.TxKind.EXPENSE) = "edit/$id?kind=${kind.name}"
    fun bill(id: Long) = "bill/$id"
    fun person(id: Long) = "person/$id"
}

/**
 * A snackbar that leaves after about three and a half seconds. The stock Short and Long lengths (4 s and 10 s)
 * kept Undo hanging around, and a second delete used to wait behind the first.
 */
private suspend fun SnackbarHostState.showBrief(message: String, actionLabel: String? = null): SnackbarResult =
    kotlinx.coroutines.withTimeoutOrNull(3_500) {
        showSnackbar(message, actionLabel, duration = androidx.compose.material3.SnackbarDuration.Indefinite)
    } ?: SnackbarResult.Dismissed

@Composable
fun AppRoot(spec: ThemeSpec, onTheme: ((ThemeSpec) -> ThemeSpec) -> Unit, vm: BudgetViewModel, launch: LaunchRequest? = null, onLaunchHandled: () -> Unit = {}) {
    val state by vm.state.collectAsState()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Route.HOME, Route.ACTIVITY, Route.INSIGHTS, Route.BILLS)
    // Budgets, Goals, People and Recently deleted live behind the fifth item, More.
    var moreOpen by remember { mutableStateOf(false) }
    val showBar = route in tabs || route == Route.BUDGETS
    androidx.compose.runtime.LaunchedEffect(route) { moreOpen = false }
    androidx.activity.compose.BackHandler(enabled = moreOpen) { moreOpen = false }
    val m = Furi.motion
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.deleted)
    val undo = stringResource(R.string.undo)

    val resources = androidx.compose.ui.platform.LocalResources.current
    var focusDay by remember { mutableStateOf<Long?>(null) }
    androidx.compose.runtime.LaunchedEffect(launch) {
        if (launch == null) return@LaunchedEffect
        when (launch.route) {
            "add_expense" -> nav.navigate(Route.edit(0))
            "add_income" -> nav.navigate(Route.edit(0, app.furibudget.data.TxKind.INCOME))
            "template" -> launch.id?.let { id ->
                val t = vm.quickAddById(id)
                if (t != null) snackbar.showBrief(resources.getString(R.string.added_template, t.name))
            }
        }
        if (launch.route == "bills") {
            focusDay = launch.day
            nav.navigate(Route.BILLS) { popUpTo(Route.HOME) { saveState = false }; launchSingleTop = true }
        }
        onLaunchHandled()
    }
    var paying by remember { mutableStateOf<app.furibudget.data.BillOccurrence?>(null) }

    /** Takes a payment back and offers Undo, which puts the expense and the payment back together. */
    fun unpayWithUndo(p: app.furibudget.data.BillPaymentEntity, name: String) {
        vm.unpay(p) { (tx, pay) ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showBrief(resources.getString(R.string.bill_unpaid_msg, name), undo) == SnackbarResult.ActionPerformed) {
                    vm.redoPayment(tx, pay)
                }
            }
        }
    }

    val fb = app.furibudget.feedback.LocalFeedback.current
    val deleteWithUndo: (app.furibudget.data.TransactionEntity) -> Unit = { tx ->
        fb.deleted()
        vm.delete(tx)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showBrief(deletedMsg, undo) == SnackbarResult.ActionPerformed) {
                vm.restore(tx)
            }
        }
    }

    val billDeletedMsg = stringResource(R.string.bill_deleted)
    val deleteBillWithUndo: (app.furibudget.data.BillEntity) -> Unit = { b ->
        fb.deleted()
        vm.deleteBill(b)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showBrief(billDeletedMsg, undo) == SnackbarResult.ActionPerformed) vm.restoreBill(b.id)
        }
    }

    Box(Modifier.fillMaxSize()) {
        FuriBackground()
        // Tablets and landscape: keep content in a phone-width column in the middle.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Route.HOME,
            // Plain tweens, not springs: a spring keeps the old screen on top for most of a second while it settles,
            // and that tail is what made Back feel late.
            enterTransition = { fadeIn(m.tween(180)) + slideInHorizontally(m.tween(220)) { it / 14 } },
            exitTransition = { fadeOut(m.tween(110)) + slideOutHorizontally(m.tween(180)) { -it / 14 } },
            popEnterTransition = { fadeIn(m.tween(180)) + slideInHorizontally(m.tween(220)) { -it / 14 } },
            popExitTransition = { fadeOut(m.tween(100)) + slideOutHorizontally(m.tween(160)) { it / 14 } },
        ) {
            composable(Route.HOME) {
                HomeScreen(state, onOpenAppearance = { nav.navigate(Route.APPEARANCE) }, onOpenGoals = { nav.navigate(Route.GOALS) }, onOpenPeople = { nav.navigate(Route.PEOPLE) }, onOpenBills = { nav.navigate(Route.BILLS) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } }, onOpenSettings = { nav.navigate(Route.SETTINGS) }, onOpenBudgets = { nav.navigate(Route.BUDGETS) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } }, onOpenTx = { nav.navigate(Route.edit(it)) }, onQuickAdd = { t -> vm.quickAdd(t) { tx -> scope.launch { snackbar.currentSnackbarData?.dismiss(); if (snackbar.showBrief(resources.getString(R.string.added_template, t.name), undo) == SnackbarResult.ActionPerformed) vm.delete(tx) } } })
            }
            composable(Route.ACTIVITY) {
                ActivityScreen(
                    state,
                    onOpenTx = { id ->
                        // Money lent, borrowed or paid back is managed on the person's page, not in the plain editor.
                        val t = state.transactions.firstOrNull { it.id == id }
                        val loan = t?.loanId?.let { lid -> state.loans.firstOrNull { it.id == lid } }
                        when {
                            loan != null -> nav.navigate(Route.person(loan.personId))
                            t?.role == app.furibudget.data.TxRole.GOAL -> nav.navigate(Route.GOALS)
                            t != null && t.role != app.furibudget.data.TxRole.NORMAL && t.role != app.furibudget.data.TxRole.ADJUSTMENT -> Unit // its person was deleted
                            else -> nav.navigate(Route.edit(id))
                        }
                    },
                    onDelete = deleteWithUndo,
                )
            }
            composable(Route.INSIGHTS) { InsightsScreen(state) }
            composable(Route.BUDGETS) { BudgetsScreen(state, vm::saveBudget, vm::deleteBudget) }
            composable(Route.GOALS) { GoalsScreen(state, vm::saveFund, vm::deleteFund, vm::moveFund) { nav.popBackStack() } }
            composable(Route.BILLS) {
                BillsScreen(
                    state,
                    onPay = { paying = it },
                    onUnpay = { occ -> occ.payment?.let { p -> unpayWithUndo(p, occ.bill.name) } },
                    onSkip = { vm.skipBill(it.bill, it.due) },
                    onClearSkip = { occ -> occ.payment?.let(vm::clearPayment) },
                    onEditBill = { nav.navigate(Route.bill(it)) },
                    onDeleteBill = deleteBillWithUndo,
                    focusDay = focusDay,
                )
            }
            composable(
                Route.BILL_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { fadeIn(m.tween(180)) + slideInVertically(m.tween(240)) { it / 8 } },
                popExitTransition = { fadeOut(m.tween(100)) + slideOutVertically(m.tween(170)) { it / 8 } },
            ) { entry ->
                BillEditScreen(entry.arguments?.getLong("id") ?: 0L, state, onSave = vm::saveBill, onDelete = vm::deleteBill, onClose = { nav.popBackStack() })
            }
            composable(Route.APPEARANCE) { AppearanceScreen(spec, onTheme) { nav.popBackStack() } }
            composable(Route.SETTINGS) {
                SettingsScreen(
                    state, vm, spec, onTheme,
                    onOpenManage = { nav.navigate(Route.MANAGE) }, onOpenGoals = { nav.navigate(Route.GOALS) }, onOpenPeople = { nav.navigate(Route.PEOPLE) }, onOpenRates = { nav.navigate(Route.RATES) }, onOpenFeedback = { nav.navigate(Route.FEEDBACK) }, onOpenTrash = { nav.navigate(Route.TRASH) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Route.TRASH) {
                val trash by vm.trash.collectAsState()
                TrashScreen(
                    state, trash,
                    onRestoreFund = { vm.restoreFund(it.id) }, onForeverFund = { vm.deleteFundForever(it.id) },
                    onRestorePerson = { vm.restorePerson(it.id) }, onForeverPerson = { vm.deletePersonForever(it.id) },
                    onRestoreTx = vm::restore, onRestoreBill = { vm.restoreBill(it.id) },
                    onForeverTx = { vm.deleteForever(it) }, onForeverBill = { vm.deleteForever(it) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Route.FEEDBACK) { SoundSettingsScreen(state, vm) { nav.popBackStack() } }
            composable(Route.RATES) {
                val fetching by vm.fetchingRates.collectAsState()
                ExchangeRatesScreen(state, fetching, onUpdate = { vm.refreshRates(force = true) }, onSetManual = vm::setManualRate, onClear = vm::clearRate, onBack = { nav.popBackStack() })
            }
            composable(Route.PEOPLE) {
                PeopleScreen(state, onOpenPerson = { nav.navigate(Route.person(it)) }, onSavePerson = { p, done -> vm.savePerson(p, done) }, onBack = { nav.popBackStack() })
            }
            composable(Route.PERSON, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                PersonScreen(
                    entry.arguments?.getLong("id") ?: 0L, state,
                    onLend = { pid, d, amt, acc, due, note -> vm.lend(pid, d, amt, acc, java.time.LocalDate.now().toEpochDay(), due, note) },
                    onRepay = { lid, amt, acc -> vm.repay(lid, amt, acc, java.time.LocalDate.now().toEpochDay()) }, onSavePerson = { vm.savePerson(it) }, onDeletePerson = vm::deletePerson,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Route.MANAGE) {
                val checkNote = androidx.compose.ui.res.stringResource(R.string.balance_check_note)
                ManageScreen(state, vm::saveAccount, { id, app, real -> vm.checkBalance(id, app, real, checkNote) }, vm::saveCategory, vm::reorderAccounts, vm::reorderCategories, vm::deleteAccount) { nav.popBackStack() } }
            composable(
                Route.EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("kind") { type = NavType.StringType; defaultValue = "EXPENSE" }),
                enterTransition = { fadeIn(m.tween(180)) + slideInVertically(m.tween(240)) { it / 8 } },
                popExitTransition = { fadeOut(m.tween(100)) + slideOutVertically(m.tween(170)) { it / 8 } },
            ) { entry ->
                EditScreen(entry.arguments?.getLong("id") ?: 0L, state, onSave = vm::save, onAddTag = { n, done -> vm.addTag(n, 0xFF8B7CFF.toInt(), done) }, onSaveBill = vm::saveBill, onDelete = deleteWithUndo, onSaveTemplate = vm::saveTemplate, onDeleteTemplate = vm::deleteTemplate, onClose = { nav.popBackStack() },
                    initialKind = runCatching { app.furibudget.data.TxKind.valueOf(entry.arguments?.getString("kind") ?: "EXPENSE") }.getOrDefault(app.furibudget.data.TxKind.EXPENSE))
            }
        }
        // Fades content under the status bar so the clock never collides with scrolled text.
        }
        }
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            Modifier.fillMaxWidth().height(top + 14.dp)
                .background(Brush.verticalGradient(listOf(Furi.colors.background.copy(alpha = 0.78f), Color.Transparent))),
        )
        val onboarding = state.loaded && !state.settings.onboarded
        if (onboarding) {
            // Opaque: the screens underneath must not show through the pager.
            Box(Modifier.fillMaxSize()) {
                FuriBackground()
                OnboardingScreen(state, spec, onTheme, vm::saveAccount) { vm.updateSettings { it.copy(onboarded = true) } }
            }
        }
        if (moreOpen) {
            // Tapping anywhere outside the menu closes it.
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { moreOpen = false })
        }
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 640.dp).navigationBarsPadding()) {
            SnackbarHost(snackbar, Modifier.padding(horizontal = 16.dp))
            // Right edge lines up with the bar, not the add button beside it (68dp button + 12dp gap).
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 96.dp), horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = moreOpen && showBar && !onboarding,
                    enter = fadeIn(m.tween(140)) + slideInVertically(m.tween(200)) { it / 3 } + androidx.compose.animation.scaleIn(m.tween(200), initialScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 1f)),
                    exit = fadeOut(m.tween(100)) + slideOutVertically(m.tween(140)) { it / 4 },
                ) {
                    MoreMenu(
                        listOf(
                            MoreEntry(stringResource(R.string.tab_budgets), Icons.Rounded.AccountBalanceWallet, Route.BUDGETS),
                            MoreEntry(stringResource(R.string.tab_goals), Icons.Rounded.Flag, Route.GOALS),
                            MoreEntry(stringResource(R.string.people), Icons.Rounded.Group, Route.PEOPLE),
                            MoreEntry(stringResource(R.string.recently_deleted), Icons.Rounded.DeleteSweep, Route.TRASH),
                        ),
                        current = route,
                    ) { target ->
                        moreOpen = false
                        nav.navigate(target) { launchSingleTop = true }
                    }
                }
            }
            AnimatedVisibility(
                visible = showBar && !onboarding,
                enter = fadeIn(m.tween(200)) + slideInVertically(m.spring()) { it },
                exit = fadeOut(m.tween(120)) + slideOutVertically(m.tween(160)) { it },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FuriNavBar(
                        items = listOf(NavItem(stringResource(R.string.tab_home), Icons.Rounded.Home), NavItem(stringResource(R.string.tab_activity), Icons.Rounded.Receipt), NavItem(stringResource(R.string.tab_insights), Icons.Rounded.PieChart), NavItem(stringResource(R.string.tab_bills), Icons.Rounded.CalendarMonth), NavItem(stringResource(R.string.tab_more), Icons.Rounded.MoreHoriz)),
                        selected = if (moreOpen || route == Route.BUDGETS) tabs.size else tabs.indexOf(route).coerceAtLeast(0),
                        onSelect = { i ->
                            if (i == tabs.size) { moreOpen = !moreOpen; return@FuriNavBar }
                            moreOpen = false
                            nav.navigate(tabs[i]) {
                                popUpTo(Route.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Furi.colors.accentBrush)
                            .clickable { nav.navigate(if (route == Route.BILLS) Route.bill(0) else Route.edit(0)) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, stringResource(if (route == Route.BILLS) R.string.add_bill else R.string.add_transaction), tint = Furi.colors.onAccent) }
                }
            }
        }
    }

    paying?.let { occ ->
        PaySheet(
            state, occ,
            onConfirm = { amount, account, date ->
                paying = null
                vm.payBill(occ.bill, occ.due, amount, account, date) { payment ->
                    fb.paid()
                    if (payment != null) scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        if (snackbar.showBrief(resources.getString(R.string.bill_paid_msg, occ.bill.name), undo) == SnackbarResult.ActionPerformed) {
                            unpayWithUndo(payment, occ.bill.name)
                        }
                    }
                }
            },
            onDismiss = { paying = null },
        )
    }
}
