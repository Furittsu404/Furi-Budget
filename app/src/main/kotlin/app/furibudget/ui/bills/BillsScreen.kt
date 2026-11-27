package app.furibudget.ui.bills

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.BillSchedule
import app.furibudget.core.BillStatus
import app.furibudget.core.Money
import app.furibudget.data.BillEntity
import app.furibudget.data.BillOccurrence
import app.furibudget.data.BillType
import app.furibudget.data.Bills
import app.furibudget.data.TxKind
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.components.FuriIcons
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

private enum class BillFilter { ALL, BILLS, SUBS, INCOME }

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayTitle = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

private fun BillOccurrence.matches(f: BillFilter) = when (f) {
    BillFilter.ALL -> true
    BillFilter.BILLS -> bill.kind != TxKind.INCOME && bill.type != BillType.SUBSCRIPTION
    BillFilter.SUBS -> bill.type == BillType.SUBSCRIPTION
    BillFilter.INCOME -> bill.kind == TxKind.INCOME
}

/** What this date is worth: what was paid, or what was expected. */
private fun BillOccurrence.amount(): Long = payment?.amountMinor?.takeIf { it > 0 } ?: bill.amountMinor

@Composable
fun BillsScreen(
    state: BudgetState,
    onPay: (BillOccurrence) -> Unit,
    onUnpay: (BillOccurrence) -> Unit,
    onSkip: (BillOccurrence) -> Unit,
    onClearSkip: (BillOccurrence) -> Unit,
    onEditBill: (Long) -> Unit,
    focusDay: Long? = null,
) {
    val c = Furi.colors
    val today = LocalDate.now()
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableLongStateOf(focusDay ?: today.toEpochDay()) }
    var filter by rememberSaveable { mutableStateOf(BillFilter.ALL) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val month = remember(offset) { YearMonth.from(today).plusMonths(offset.toLong()) }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val firstDow = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val gridStart = remember(month) { month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDow)) }
    val all = remember(state.bills, state.billPayments, gridStart) { Bills.occurrences(state.bills, state.billPayments, gridStart, gridStart.plusDays(41), today) }
    val inGrid = remember(all, filter) { all.filter { it.matches(filter) } }
    val overdue = remember(state.bills, state.billPayments, filter) { Bills.overdue(state.bills, state.billPayments, today).filter { it.matches(filter) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 168.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.tab_bills), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showAll, onClick = { showAll = false }, label = { Text(stringResource(R.string.view_calendar)) })
            FilterChip(selected = showAll, onClick = { showAll = true }, label = { Text(stringResource(R.string.view_all_bills)) })
        }
        if (state.bills.isEmpty()) {
            FuriCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.no_bills_yet), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
            }
        } else if (showAll) {
            AllBills(state, today, onEditBill)
        } else {
            if (overdue.isNotEmpty()) {
                val total = overdue.sumOf { it.amount() }
                FuriCard(Modifier.fillMaxWidth().clickable { selected = overdue.first().due.toEpochDay(); offset = 0 }) {
                    Text(
                        stringResource(R.string.overdue_banner, overdue.size, state.currency.format(Money(total), true)),
                        Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall, color = c.expense,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(BillFilter.ALL to R.string.filter_all, BillFilter.BILLS to R.string.filter_bills, BillFilter.SUBS to R.string.filter_subs, BillFilter.INCOME to R.string.filter_income).forEach { (f, l) ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(stringResource(l)) })
                }
            }
            MonthSummary(state, inGrid, month)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset -= 1 }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.prev_month)) }
                Text(month.atDay(1).format(monthTitle), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = c.onSurface, textAlign = TextAlign.Center)
                IconButton(onClick = { offset += 1 }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month)) }
            }
            val m = Furi.motion
            AnimatedContent(
                targetState = offset,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(m.spring()) { dir * it / 4 } + fadeIn(m.tween(200))) togetherWith
                        (slideOutHorizontally(m.spring()) { -dir * it / 4 } + fadeOut(m.tween(120))) using SizeTransform(clip = false)
                },
                label = "month",
            ) { off ->
                // Each page builds its own month, so the old one keeps its dates while it slides out.
                val pageMonth = YearMonth.from(today).plusMonths(off.toLong())
                val pageStart = pageMonth.atDay(1).with(TemporalAdjusters.previousOrSame(firstDow))
                val pageOcc = remember(state.bills, state.billPayments, pageStart, filter) {
                    Bills.occurrences(state.bills, state.billPayments, pageStart, pageStart.plusDays(41), today).filter { it.matches(filter) }
                }
                CalendarGrid(locale, pageMonth, pageStart, firstDow, pageOcc, today, selected) { selected = it.toEpochDay() }
            }
            val day = LocalDate.ofEpochDay(selected)
            Text(day.format(dayTitle), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            val onDay = inGrid.filter { it.due == day }
            if (onDay.isEmpty()) {
                Text(stringResource(R.string.nothing_due), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
            onDay.forEach { BillRow(it, state, today, onPay, onUnpay, onSkip, onClearSkip, onEditBill) }
            val soon = remember(all, filter, selected) {
                Bills.upcoming(state.bills, state.billPayments, today, 30).filter { it.matches(filter) && it.due != day && !it.due.isBefore(today) }
            }
            if (soon.isNotEmpty()) {
                Text(stringResource(R.string.next_30_days), style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.padding(top = 8.dp))
                soon.forEach { BillRow(it, state, today, onPay, onUnpay, onSkip, onClearSkip, onEditBill) }
            }
        }
    }
}

@Composable
private fun MonthSummary(state: BudgetState, occ: List<BillOccurrence>, month: YearMonth) {
    val c = Furi.colors
    val cur = state.currency
    val inMonth = occ.filter { YearMonth.from(it.due) == month && it.bill.kind != TxKind.INCOME && it.status != BillStatus.SKIPPED }
    val due = inMonth.sumOf { it.amount() }
    val paid = inMonth.filter { it.status == BillStatus.PAID }.sumOf { it.amount() }
    FuriCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.bills_summary_due), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                Text(cur.format(Money(due), true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.onSurface, maxLines = 1, softWrap = false)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.bills_summary_paid), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                Text(cur.format(Money(paid), true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.income, maxLines = 1, softWrap = false)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.bills_summary_left), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                Text(cur.format(Money((due - paid).coerceAtLeast(0)), true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.onSurface, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    locale: Locale,
    month: YearMonth,
    gridStart: LocalDate,
    firstDow: java.time.DayOfWeek,
    occ: List<BillOccurrence>,
    today: LocalDate,
    selected: Long,
    onSelect: (LocalDate) -> Unit,
) {
    val c = Furi.colors
    val byDay = remember(occ) { occ.groupBy { it.due } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { i ->
                Text(
                    firstDow.plus(i.toLong()).getDisplayName(TextStyle.SHORT, locale),
                    Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant,
                )
            }
        }
        repeat(6) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { dow ->
                    val date = gridStart.plusDays((week * 7 + dow).toLong())
                    DayCell(Modifier.weight(1f), date, YearMonth.from(date) == month, date == today, date.toEpochDay() == selected, byDay[date].orEmpty()) { onSelect(date) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(modifier: Modifier, date: LocalDate, inMonth: Boolean, isToday: Boolean, isSelected: Boolean, items: List<BillOccurrence>, onClick: () -> Unit) {
    val c = Furi.colors
    val scale by animateFloatAsState(if (isSelected) 1.06f else 1f, Furi.motion.spring(), label = "cell")
    val colors = items.map { statusColor(it.status) }
    val desc = stringResource(
        R.string.calendar_day_desc, date.format(dayTitle),
        if (items.isEmpty()) stringResource(R.string.cal_no_bills) else stringResource(R.string.cal_count_bills, items.size),
    )
    Column(
        modifier
            .height(54.dp)
            .scale(scale)
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) c.accentContainer else Color.Transparent)
            .then(if (isToday) Modifier.border(1.5.dp, c.accentText, MaterialTheme.shapes.small) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(), style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
            color = when { isSelected -> c.onAccentContainer; inMonth -> c.onSurface; else -> c.onSurfaceVariant.copy(alpha = 0.6f) },
        )
        Row(Modifier.height(8.dp).padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            colors.take(3).forEach { Box(Modifier.size(5.dp).clip(CircleShape).background(it)) }
        }
    }
}

@Composable
fun BillRow(
    occ: BillOccurrence,
    state: BudgetState,
    today: LocalDate,
    onPay: (BillOccurrence) -> Unit,
    onUnpay: (BillOccurrence) -> Unit,
    onSkip: (BillOccurrence) -> Unit,
    onClearSkip: (BillOccurrence) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val c = Furi.colors
    val b = occ.bill
    var menu by remember { mutableStateOf(false) }
    val tint = Color(b.colorArgb)
    val income = b.kind == TxKind.INCOME
    FuriCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                FuriIcon(b.iconKey(), tint = tint)
            }
            Column(Modifier.weight(1f).clickable { onEdit(b.id) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(b.name, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1)
                Text(dueLabel(occ, today) { it.format(shortDate) }, style = MaterialTheme.typography.labelMedium, color = statusColor(occ.status))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (occ.amount() > 0) state.currency.format(Money(occ.amount()), true) else stringResource(R.string.amount_varies),
                        style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = if (income) c.income else c.onSurfaceVariant,
                    )
                    if (b.autoPay) Text(stringResource(R.string.auto_pay_badge), style = MaterialTheme.typography.labelSmall, color = c.accentText)
                    if (b.paymentsTotal != null) Text(stringResource(R.string.payments_progress, occ.number, b.paymentsTotal), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                }
            }
            when (occ.status) {
                BillStatus.PAID -> TextButton(onClick = { onUnpay(occ) }) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = c.income)
                    Text(stringResource(R.string.undo_paid), Modifier.padding(start = 4.dp))
                }
                BillStatus.SKIPPED -> TextButton(onClick = { onClearSkip(occ) }) { Text(stringResource(R.string.undo_paid)) }
                else -> FilledTonalButton(onClick = { onPay(occ) }) { Text(stringResource(if (income) R.string.mark_received else R.string.mark_paid)) }
            }
            if (occ.isOpen) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.edit_bill)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.skip_this_one)) }, onClick = { menu = false; onSkip(occ) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.edit_bill)) }, onClick = { menu = false; onEdit(b.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AllBills(state: BudgetState, today: LocalDate, onEdit: (Long) -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val subs = state.bills.filter { it.active && it.type == BillType.SUBSCRIPTION }
    val monthly = subs.sumOf { BillSchedule.monthlyCost(it.amountMinor, it.repeat, it.repeatEvery) }
    if (subs.isNotEmpty()) {
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.subscriptions_cost), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                Text(stringResource(R.string.per_month_short, cur.format(Money(monthly), true)), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = c.onSurface)
                Text(stringResource(R.string.per_year_short, cur.format(Money(monthly * 12), true)), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
        }
    }
    state.bills.sortedWith(compareBy({ !it.active }, { it.type.ordinal }, { it.name.lowercase() })).forEach { b ->
        val next = Bills.upcoming(listOf(b), state.billPayments, today, 400).firstOrNull()
        FuriCard(Modifier.fillMaxWidth().clickable { onEdit(b.id) }) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(b.colorArgb).copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    FuriIcon(b.iconKey(), tint = Color(b.colorArgb))
                }
                Column(Modifier.weight(1f)) {
                    Text(b.name, style = MaterialTheme.typography.bodyLarge, color = if (b.active) c.onSurface else c.onSurfaceVariant)
                    Text(
                        billTypeLabel(b.type) + (next?.let { " · " + it.due.format(shortDate) } ?: ""),
                        style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                    )
                    if (b.paymentsTotal != null) {
                        val done = Bills.settledCount(b.id, state.billPayments)
                        Text(stringResource(R.string.payments_progress, done, b.paymentsTotal), style = MaterialTheme.typography.labelMedium, color = c.accentText)
                    }
                }
                Text(
                    if (b.amountMinor > 0) cur.format(Money(b.amountMinor), true) else stringResource(R.string.amount_varies),
                    style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = if (b.kind == TxKind.INCOME) c.income else c.onSurface,
                )
            }
        }
    }
}
