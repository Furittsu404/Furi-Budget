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
import app.furibudget.ui.components.NavItem
import app.furibudget.ui.edit.EditScreen
import app.furibudget.ui.bills.BillEditScreen
import app.furibudget.ui.bills.BillsScreen
import app.furibudget.ui.bills.PaySheet
import app.furibudget.ui.budgets.BudgetsScreen
import app.furibudget.ui.goals.GoalsScreen
import app.furibudget.ui.home.HomeScreen
import app.furibudget.ui.onboarding.OnboardingScreen
import app.furibudget.ui.people.PeopleScreen
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

@Composable
fun AppRoot(spec: ThemeSpec, onTheme: ((ThemeSpec) -> ThemeSpec) -> Unit, vm: BudgetViewModel, launch: LaunchRequest? = null, onLaunchHandled: () -> Unit = {}) {
    val state by vm.state.collectAsState()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Route.HOME, Route.ACTIVITY, Route.BUDGETS, Route.INSIGHTS, Route.BILLS)
    val showBar = route in tabs
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
                if (t != null) snackbar.showSnackbar(resources.getString(R.string.added_template, t.name))
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
                if (snackbar.showSnackbar(resources.getString(R.string.bill_unpaid_msg, name), undo, duration = androidx.compose.material3.SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                    vm.redoPayment(tx, pay)
                }
            }
        }
    }

    val deleteWithUndo: (app.furibudget.data.TransactionEntity) -> Unit = { tx ->
        vm.delete(tx)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(deletedMsg, undo, duration = androidx.compose.material3.SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                vm.restore(tx)
            }
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
            enterTransition = { fadeIn(m.tween(220)) + slideInHorizontally(m.spring()) { it / 10 } },
            exitTransition = { fadeOut(m.tween(160)) + slideOutHorizontally(m.spring()) { -it / 10 } },
            popEnterTransition = { fadeIn(m.tween(220)) + slideInHorizontally(m.spring()) { -it / 10 } },
            popExitTransition = { fadeOut(m.tween(160)) + slideOutHorizontally(m.spring()) { it / 10 } },
        ) {
            composable(Route.HOME) {
                HomeScreen(state, onOpenAppearance = { nav.navigate(Route.APPEARANCE) }, onOpenGoals = { nav.navigate(Route.GOALS) }, onOpenPeople = { nav.navigate(Route.PEOPLE) }, onOpenBills = { nav.navigate(Route.BILLS) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } }, onOpenSettings = { nav.navigate(Route.SETTINGS) }, onOpenBudgets = { nav.navigate(Route.BUDGETS) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } }, onOpenTx = { nav.navigate(Route.edit(it)) }, onQuickAdd = { t -> vm.quickAdd(t) { tx -> scope.launch { snackbar.currentSnackbarData?.dismiss(); if (snackbar.showSnackbar(resources.getString(R.string.added_template, t.name), undo, duration = androidx.compose.material3.SnackbarDuration.Long) == SnackbarResult.ActionPerformed) vm.delete(tx) } } })
            }
            composable(Route.ACTIVITY) {
                ActivityScreen(
                    state,
                    onOpenTx = { id ->
                        // Money lent, borrowed or paid back is managed on the person's page, not in the plain editor.
                        val t = state.transactions.firstOrNull { it.id == id }
                        val loan = t?.loanId?.let { lid -> state.loans.firstOrNull { it.id == lid } }
                        if (loan != null) nav.navigate(Route.person(loan.personId)) else nav.navigate(Route.edit(id))
                    },
                    onDelete = deleteWithUndo,
                )
            }
            composable(Route.INSIGHTS) { InsightsScreen(state) }
            composable(Route.BUDGETS) { BudgetsScreen(state, vm::saveBudget, vm::deleteBudget) }
            composable(Route.GOALS) { GoalsScreen(state, vm::saveGoal, vm::deleteGoal) }
            composable(Route.BILLS) {
                BillsScreen(
                    state,
                    onPay = { paying = it },
                    onUnpay = { occ -> occ.payment?.let { p -> unpayWithUndo(p, occ.bill.name) } },
                    onSkip = { vm.skipBill(it.bill, it.due) },
                    onClearSkip = { occ -> occ.payment?.let(vm::clearPayment) },
                    onEditBill = { nav.navigate(Route.bill(it)) },
                    focusDay = focusDay,
                )
            }
            composable(
                Route.BILL_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { fadeIn(m.tween(200)) + slideInVertically(m.spring()) { it / 6 } },
                popExitTransition = { fadeOut(m.tween(160)) + slideOutVertically(m.spring()) { it / 6 } },
            ) { entry ->
                BillEditScreen(entry.arguments?.getLong("id") ?: 0L, state, onSave = vm::saveBill, onDelete = vm::deleteBill, onClose = { nav.popBackStack() })
            }
            composable(Route.APPEARANCE) { AppearanceScreen(spec, onTheme) { nav.popBackStack() } }
            composable(Route.SETTINGS) {
                SettingsScreen(
                    state, vm, spec, onTheme,
                    onOpenManage = { nav.navigate(Route.MANAGE) }, onOpenGoals = { nav.navigate(Route.GOALS) }, onOpenPeople = { nav.navigate(Route.PEOPLE) }, onOpenTrash = { nav.navigate(Route.TRASH) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Route.TRASH) {
                val trash by vm.trash.collectAsState()
                TrashScreen(
                    state, trash,
                    onRestorePerson = { vm.restorePerson(it.id) }, onForeverPerson = { vm.deletePersonForever(it.id) },
                    onRestoreTx = vm::restore, onRestoreBill = { vm.restoreBill(it.id) }, onRestoreGoal = { vm.restoreGoal(it.id) },
                    onForeverTx = { vm.deleteForever(it) }, onForeverBill = { vm.deleteForever(it) }, onForeverGoal = { vm.deleteForever(it) },
                    onBack = { nav.popBackStack() },
                )
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
            composable(Route.MANAGE) { ManageScreen(state, vm::saveAccount, vm::saveCategory) { nav.popBackStack() } }
            composable(
                Route.EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("kind") { type = NavType.StringType; defaultValue = "EXPENSE" }),
                enterTransition = { fadeIn(m.tween(200)) + slideInVertically(m.spring()) { it / 6 } },
                popExitTransition = { fadeOut(m.tween(160)) + slideOutVertically(m.spring()) { it / 6 } },
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
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 640.dp).navigationBarsPadding()) {
            SnackbarHost(snackbar, Modifier.padding(horizontal = 16.dp))
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
                        items = listOf(NavItem(stringResource(R.string.tab_home), Icons.Rounded.Home), NavItem(stringResource(R.string.tab_activity), Icons.Rounded.Receipt), NavItem(stringResource(R.string.tab_budgets), Icons.Rounded.AccountBalanceWallet), NavItem(stringResource(R.string.tab_insights), Icons.Rounded.PieChart), NavItem(stringResource(R.string.tab_bills), Icons.Rounded.CalendarMonth)),
                        selected = tabs.indexOf(route).coerceAtLeast(0),
                        onSelect = { i ->
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
                    if (payment != null) scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        if (snackbar.showSnackbar(resources.getString(R.string.bill_paid_msg, occ.bill.name), undo, duration = androidx.compose.material3.SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                            unpayWithUndo(payment, occ.bill.name)
                        }
                    }
                }
            },
            onDismiss = { paying = null },
        )
    }
}
