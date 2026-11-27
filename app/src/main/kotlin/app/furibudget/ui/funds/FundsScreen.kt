package app.furibudget.ui.funds

import androidx.compose.foundation.horizontalScroll
import app.furibudget.ui.theme.entityColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.rounded.Event
import app.furibudget.data.HoldMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.FundEntity
import app.furibudget.data.FundStatus
import app.furibudget.data.Funds
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.ConfettiBurst
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.components.LiquidFill
import app.furibudget.ui.settings.ColorRow
import app.furibudget.ui.settings.IconButtonRow
import app.furibudget.ui.theme.Accents
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate

/** Savings goals: what you are saving toward, where the money sits, and what that leaves free to spend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(state: BudgetState, onSaveFund: (FundEntity) -> Unit, onDeleteFund: (Long) -> Unit, onMove: (goalId: Long, deltaMinor: Long, accountId: Long?) -> Unit, onBack: () -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val statuses = remember(state.funds, state.fundMoves, state.transactions) { Funds.statuses(state.funds, state.fundMoves, state.transactions.map { it.id }.toSet()) }
    val ftsMoney = remember(state.total, statuses, state.bills, state.billPayments) { freeToSpend(state) }
    val movedOut = remember(statuses) { Funds.movedOut(statuses) }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }   // -1 = new
    var moving by rememberSaveable { mutableStateOf<Long?>(null) }
    var movingOut by rememberSaveable { mutableStateOf(false) }
    var confetti by rememberSaveable { mutableIntStateOf(0) }
    val reachedNow = statuses.count { it.reached }
    var seen by rememberSaveable { mutableIntStateOf(reachedNow) }
    val fb = app.furibudget.feedback.LocalFeedback.current
    LaunchedEffect(reachedNow) { if (reachedNow > seen) { confetti++; fb.goal() }; seen = reachedNow }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.funds), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                IconButton(onClick = { editing = -1L }) { Icon(Icons.Rounded.Add, stringResource(R.string.add_fund)) }
            }
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.free_to_spend), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    AnimatedAmount(
                        cur.symbol + " " + cur.format(Money(ftsMoney.freeMinor)),
                        Furi.figures.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                        if (ftsMoney.freeMinor < 0) c.expense else c.onSurface,
                    )
                    FreeBar(ftsMoney.freeMinor, ftsMoney.billsDueMinor, ftsMoney.reservedMinor)
                    Text(
                        stringResource(R.string.free_breakdown, cur.format(Money(ftsMoney.totalMinor), true), cur.format(Money(ftsMoney.reservedMinor), true), cur.format(Money(ftsMoney.billsDueMinor), true)),
                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                    )
                    if (movedOut > 0) Text(stringResource(R.string.goals_moved_total, cur.format(Money(movedOut), true)), style = MaterialTheme.typography.bodySmall, color = c.accentText)
                }
            }
            if (statuses.isEmpty()) EmptyFunds { editing = -1L }
            statuses.forEach { st -> FundCard(st, state, onEdit = { editing = st.fund.id }, onAdd = { moving = st.fund.id; movingOut = false }, onTake = { moving = st.fund.id; movingOut = true }) }
        }
        ConfettiBurst(confetti, listOf(c.income, c.accent, c.warning), Modifier.fillMaxSize())
    }

    editing?.let { id ->
        FundDialog(statuses.firstOrNull { it.fund.id == id }?.fund, statuses.firstOrNull { it.fund.id == id }?.savedMinor ?: 0L, state, onDismiss = { editing = null }, onDelete = { onDeleteFund(it); editing = null }, onSave = { onSaveFund(it); editing = null })
    }
    moving?.let { id ->
        val st = statuses.firstOrNull { it.fund.id == id }
        if (st == null) moving = null else MoveSheet(st, state, movingOut, onDismiss = { moving = null }) { amt, acc -> onMove(id, if (movingOut) -amt else amt, acc); moving = null }
    }
}

/** What each open account holds, has set aside and has free this month. */
fun accountMoney(state: BudgetState): List<app.furibudget.data.AccountMoney> {
    val statuses = Funds.statuses(state.funds, state.fundMoves, state.transactions.map { it.id }.toSet())
    val today = LocalDate.now()
    return Funds.perAccount(state.accounts, state.balances.mapValues { it.value.minor }, statuses, state.bills, state.billPayments, today, state.month(today).end)
}

/** The same picture for all open accounts together. */
fun freeToSpend(state: BudgetState): app.furibudget.data.FreeToSpend = Funds.total(accountMoney(state))

/** Free (accent), bills still due (warning) and reserved (income) as shares of the money you hold. */
@Composable
fun FreeBar(free: Long, billsDue: Long, reserved: Long, modifier: Modifier = Modifier) {
    val c = Furi.colors
    // With minimal colours the three parts are steps of the accent and ink instead of three hues.
    val min = Furi.spec.minimal
    val parts = listOf(
        free.coerceAtLeast(0) to c.accent,
        billsDue.coerceAtLeast(0) to if (min) c.onSurface.copy(alpha = 0.55f) else c.warning,
        reserved.coerceAtLeast(0) to if (min) c.accent.copy(alpha = 0.4f) else c.income,
    )
    val sum = parts.sumOf { it.first }
    Row(modifier.fillMaxWidth().height(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.onSurface.copy(alpha = 0.1f)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (free < 0) Box(Modifier.fillMaxSize().background(if (min) c.onSurface else c.expense)) else if (sum > 0) parts.filter { it.first > 0 }.forEach { (v, col) -> Box(Modifier.weight(v.toFloat()).fillMaxSize().background(col)) }
    }
}

@Composable
private fun FundCard(st: FundStatus, state: BudgetState, onEdit: () -> Unit, onAdd: () -> Unit, onTake: () -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val tint = entityColor(st.fund.colorArgb)
    val coin = remember { Animatable(1f) }
    var lastSaved by remember { mutableStateOf(st.savedMinor) }
    val motionOn = Furi.motion.enabled
    LaunchedEffect(st.savedMinor) {
        if (st.savedMinor > lastSaved && motionOn) { coin.snapTo(0f); coin.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        lastSaved = st.savedMinor
    }
    FuriCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(width = 64.dp, height = 84.dp).clip(MaterialTheme.shapes.large).border(2.dp, tint.copy(alpha = 0.6f), MaterialTheme.shapes.large)) {
                    LiquidFill(if (st.fund.targetMinor == null) (if (st.savedMinor > 0) 0.35f else 0f) else st.fraction, tint.copy(alpha = 0.75f), Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { FuriIcon(st.fund.icon, tint = c.onSurface.copy(alpha = 0.85f), size = 28.dp) }
                    if (coin.value < 1f) Canvas(Modifier.fillMaxSize()) {
                        val y = -8f + (size.height * 0.55f) * coin.value
                        drawCircle(Color(0xFFFFD54F).copy(alpha = 1f - coin.value * 0.7f), 7.dp.toPx(), Offset(size.width / 2, y))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(st.fund.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = c.onSurface, maxLines = 1)
                        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit_fund), Modifier.size(20.dp)) }
                    }
                    AnimatedAmount(cur.symbol + " " + cur.format(Money(st.savedMinor)), Furi.figures.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), c.onSurface)
                    if (st.fund.holdMode == HoldMode.MOVE_OUT) Text(stringResource(R.string.goal_moved_out_label), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    else state.account(st.fund.accountId)?.let { Text(stringResource(R.string.fund_held_in_name, it.name), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant) }
                    st.fund.targetMinor?.let {
                        Text(stringResource(R.string.fund_of_target, cur.format(Money(it), true), (st.fraction * 100).toInt()), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                    st.fund.monthlyMinor?.takeIf { it > 0 }?.let {
                        Text(stringResource(R.string.fund_monthly, cur.format(Money(it), true)), style = MaterialTheme.typography.labelSmall, color = c.accentText)
                    }
                    val plan = remember(st) { Funds.plan(st, LocalDate.now()) }
                    st.fund.deadlineEpochDay?.let { d ->
                        val date = LocalDate.ofEpochDay(d).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                        when {
                            plan.reached -> Text(stringResource(R.string.goal_reached), style = MaterialTheme.typography.labelSmall, color = c.accentText)
                            plan.overdue -> Text(stringResource(R.string.goal_overdue, date), style = MaterialTheme.typography.labelSmall, color = c.expense)
                            plan.monthlyNeededMinor != null -> Text(stringResource(R.string.goal_per_month, cur.format(Money(plan.monthlyNeededMinor), true), date), style = MaterialTheme.typography.labelSmall, color = c.accentText)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onAdd, Modifier.weight(1f)) { Text(stringResource(R.string.add_money)) }
                FilledTonalButton(onClick = onTake, Modifier.weight(1f), enabled = st.savedMinor > 0) { Text(stringResource(R.string.take_out)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FundDialog(existing: FundEntity?, saved: Long, state: BudgetState, onDismiss: () -> Unit, onDelete: (Long) -> Unit, onSave: (FundEntity) -> Unit) {
    val cur = state.currency
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var account by rememberSaveable { mutableStateOf(existing?.accountId ?: state.accounts.firstOrNull()?.id ?: 0L) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "savings") }
    var color by rememberSaveable { mutableIntStateOf(existing?.colorArgb ?: Accents.swatches[5].argb) }
    var target by rememberSaveable { mutableStateOf(existing?.targetMinor?.let { cur.format(Money(it)).replace(",", "") }.orEmpty()) }
    var monthly by rememberSaveable { mutableStateOf(existing?.monthlyMinor?.let { cur.format(Money(it)).replace(",", "") }.orEmpty()) }
    var moveOut by rememberSaveable { mutableStateOf(existing?.holdMode == HoldMode.MOVE_OUT) }
    var deadline by rememberSaveable { mutableStateOf(existing?.deadlineEpochDay) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    // Where the money lives cannot change once there is money in the goal: past moves were made under the old rule.
    val modeLocked = existing != null && saved != 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_fund else R.string.edit_fund)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(30) }, singleLine = true, label = { Text(stringResource(R.string.fund_name)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.goal_mode), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(selected = !moveOut, enabled = !modeLocked || !moveOut, onClick = { moveOut = false }, label = { Text(stringResource(R.string.goal_mode_reserve)) })
                    androidx.compose.material3.FilterChip(selected = moveOut, enabled = !modeLocked || moveOut, onClick = { moveOut = true; monthly = "" }, label = { Text(stringResource(R.string.goal_mode_move)) })
                }
                Text(
                    stringResource(if (modeLocked) R.string.goal_mode_locked else if (moveOut) R.string.goal_mode_move_hint else R.string.goal_mode_reserve_hint),
                    style = MaterialTheme.typography.bodySmall, color = Furi.colors.onSurfaceVariant,
                )
                if (!moveOut) {
                    Text(stringResource(R.string.fund_held_in), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.accounts.forEach { a ->
                            androidx.compose.material3.FilterChip(selected = account == a.id, onClick = { account = a.id }, label = { Text(a.name) }, leadingIcon = { FuriIcon(a.icon, size = 18.dp, tint = entityColor(a.colorArgb)) })
                        }
                    }
                }
                IconButtonRow(icon, Color(color)) { icon = it }
                ColorRow(color) { color = it }
                AmountField(target, { target = it }, cur, stringResource(R.string.fund_target_optional), Modifier.fillMaxWidth())
                if (!moveOut) AmountField(monthly, { monthly = it }, cur, stringResource(R.string.fund_monthly_optional), Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = deadline != null, onClick = { pickDate = true },
                        label = { Text(deadline?.let { LocalDate.ofEpochDay(it).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) } ?: stringResource(R.string.goal_no_deadline)) },
                        leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) },
                    )
                    if (deadline != null) TextButton(onClick = { deadline = null }) { Text(stringResource(R.string.clear)) }
                }
                if (existing != null) TextButton(onClick = { onDelete(existing.id) }) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = Furi.colors.expense)
                    Text(stringResource(R.string.delete), Modifier.padding(start = 6.dp), color = Furi.colors.expense)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(
                    (existing ?: FundEntity(name = "", colorArgb = color)).copy(
                        name = name.trim(), icon = icon, colorArgb = color, accountId = account.takeIf { it != 0L },
                        targetMinor = cur.parse(target)?.minor?.takeIf { it > 0 }, monthlyMinor = if (moveOut) null else cur.parse(monthly)?.minor?.takeIf { it > 0 },
                        holdMode = if (moveOut) HoldMode.MOVE_OUT else HoldMode.RESERVE, deadlineEpochDay = deadline,
                    ),
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (pickDate) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = (deadline ?: LocalDate.now().plusMonths(3).toEpochDay()) * 86_400_000L)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { deadline = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { androidx.compose.material3.DatePicker(pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveSheet(st: FundStatus, state: BudgetState, out: Boolean, onDismiss: () -> Unit, onSave: (Long, Long?) -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    var amount by rememberSaveable { mutableStateOf("") }
    val movesMoney = st.fund.holdMode == HoldMode.MOVE_OUT
    var account by rememberSaveable { mutableStateOf(state.accounts.firstOrNull()?.id ?: 0L) }
    val asked = cur.parse(amount)?.minor ?: 0L
    val amt = if (out) Funds.capTakeOut(asked, st) else asked
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (out) R.string.take_out else R.string.add_money) + " · " + st.fund.name, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            Text(stringResource(if (movesMoney) R.string.goal_money_hint else R.string.fund_move_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            AmountField(amount, { amount = it }, cur, stringResource(R.string.amount), Modifier.fillMaxWidth())
            if (movesMoney) {
                Text(stringResource(R.string.goal_money_account), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.accounts.forEach { a ->
                        androidx.compose.material3.FilterChip(
                            selected = account == a.id, onClick = { account = a.id },
                            label = { Text(a.name + " · " + cur.format(state.balances[a.id] ?: Money.ZERO)) },
                            leadingIcon = { FuriIcon(a.icon, size = 18.dp, tint = entityColor(a.colorArgb)) },
                        )
                    }
                }
            }
            if (out && asked > st.savedMinor) Text(stringResource(R.string.more_than_in_fund, cur.format(Money(st.savedMinor), true)), style = MaterialTheme.typography.bodySmall, color = c.expense)
            Button(enabled = amt > 0 && (!out || asked <= st.savedMinor) && (!movesMoney || account != 0L), onClick = { onSave(amt, account.takeIf { movesMoney }) }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text(stringResource(R.string.save)) }
        }
    }
}

/** A jar with a coin dropping in, in the accent colour. */
@Composable
private fun EmptyFunds(onAdd: () -> Unit) {
    val c = Furi.colors
    val drop = remember { Animatable(0f) }
    val motionOn = Furi.motion.enabled
    LaunchedEffect(Unit) {
        if (motionOn) while (true) { drop.snapTo(0f); drop.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)); kotlinx.coroutines.delay(500) } else drop.snapTo(0.6f)
    }
    val accent = c.accentText
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(Modifier.size(width = 120.dp, height = 140.dp)) {
            val jarW = size.width * 0.62f; val left = (size.width - jarW) / 2; val top = size.height * 0.28f; val bottom = size.height - 6f
            val st = androidx.compose.ui.graphics.drawscope.Stroke(5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawRoundRect(accent.copy(alpha = 0.18f), Offset(left, top), androidx.compose.ui.geometry.Size(jarW, bottom - top), androidx.compose.ui.geometry.CornerRadius(26f))
            drawRoundRect(accent, Offset(left, top), androidx.compose.ui.geometry.Size(jarW, bottom - top), androidx.compose.ui.geometry.CornerRadius(26f), style = st)
            drawLine(accent, Offset(left - 6f, top), Offset(left + jarW + 6f, top), 5f, androidx.compose.ui.graphics.StrokeCap.Round)
            val y = 0f + (bottom - 34f) * drop.value
            drawCircle(Color(0xFFFFD54F).copy(alpha = if (drop.value > 0.9f) (1f - drop.value) * 10f else 1f), 12f, Offset(size.width / 2, y))
        }
        Text(stringResource(R.string.funds_empty), style = MaterialTheme.typography.titleMedium, color = c.onSurface, textAlign = TextAlign.Center)
        Text(stringResource(R.string.funds_empty_hint), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onAdd) { Text(stringResource(R.string.add_fund)) }
    }
}
