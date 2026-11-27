package app.furibudget.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.BillSchedule
import app.furibudget.core.Money
import app.furibudget.core.Repeat
import app.furibudget.data.BillEntity
import app.furibudget.data.BillType
import app.furibudget.data.Bills
import app.furibudget.data.PayStatus
import app.furibudget.data.TxKind
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.BudgetRing
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.components.FuriIcons
import app.furibudget.ui.components.plain
import app.furibudget.ui.components.repeatLabel
import app.furibudget.ui.theme.Furi
import app.furibudget.data.Budgeting
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

private val typeColors = mapOf(
    BillType.BILL to 0xFFFF6B6B.toInt(), BillType.SUBSCRIPTION to 0xFFE066FF.toInt(), BillType.INSTALMENT to 0xFFFBA31E.toInt(),
    BillType.LOAN to 0xFF38BDF8.toInt(), BillType.INCOME to 0xFF4ADE80.toInt(), BillType.RECURRING to 0xFF8B7CFF.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillEditScreen(billId: Long, state: BudgetState, onSave: (BillEntity) -> Unit, onDelete: (BillEntity) -> Unit, onClose: () -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val existing = remember(billId, state.loaded, state.bills) { state.bills.firstOrNull { it.id == billId } }
    val key = arrayOf<Any?>(billId, existing != null, state.accounts.isNotEmpty())

    var name by rememberSaveable(*key) { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable(*key) { mutableStateOf(existing?.type ?: BillType.BILL) }
    var amountText by rememberSaveable(*key) { mutableStateOf(existing?.amountMinor?.takeIf { it > 0 }?.let { cur.plain(Money(it)) }.orEmpty()) }
    var varies by rememberSaveable(*key) { mutableStateOf(existing != null && existing.amountMinor == 0L) }
    var accountId by rememberSaveable(*key) { mutableLongStateOf(existing?.accountId ?: state.accounts.firstOrNull()?.id ?: 0L) }
    var categoryId by rememberSaveable(*key) { mutableStateOf(existing?.categoryId) }
    var dueDay by rememberSaveable(*key) { mutableLongStateOf(existing?.firstDueEpochDay ?: LocalDate.now().plusDays(7).toEpochDay()) }
    var repeat by rememberSaveable(*key) { mutableStateOf(existing?.repeat ?: Repeat.MONTHLY) }
    var every by rememberSaveable(*key) { mutableIntStateOf(existing?.repeatEvery ?: 1) }
    var endMode by rememberSaveable(*key) { mutableIntStateOf(if (existing?.paymentsTotal != null) 1 else if (existing?.endEpochDay != null) 2 else 0) }
    var paymentsText by rememberSaveable(*key) { mutableStateOf(existing?.paymentsTotal?.toString().orEmpty()) }
    var endDay by rememberSaveable(*key) { mutableLongStateOf(existing?.endEpochDay ?: LocalDate.now().plusYears(1).toEpochDay()) }
    var remind by rememberSaveable(*key) { mutableIntStateOf(existing?.remindDaysBefore ?: 3) }
    var autoPay by rememberSaveable(*key) { mutableStateOf(existing?.autoPay ?: false) }
    var note by rememberSaveable(*key) { mutableStateOf(existing?.note.orEmpty()) }
    var icon by rememberSaveable(*key) { mutableStateOf(existing?.icon ?: "bills") }
    var pickingDue by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }

    val income = type == BillType.INCOME
    val amount = if (varies) Money.ZERO else cur.parse(amountText)
    val payments = paymentsText.toIntOrNull()
    val valid = name.isNotBlank() && accountId != 0L && (varies || (amount != null && amount.minor > 0)) &&
        (endMode != 1 || (payments != null && payments > 0)) && !(autoPay && varies)

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            Text(stringResource(if (existing == null) R.string.new_bill else R.string.edit_bill), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            if (existing != null) IconButton(onClick = { onDelete(existing); onClose() }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = c.expense) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(BillType.BILL, BillType.SUBSCRIPTION, BillType.INSTALMENT, BillType.LOAN, BillType.INCOME).forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            type = t
                            if (t == BillType.INSTALMENT && endMode == 0) endMode = 1
                            if (t == BillType.INCOME && categoryId != null && state.category(categoryId)?.kind != TxKind.INCOME) categoryId = null
                            if (t != BillType.INCOME && state.category(categoryId)?.kind == TxKind.INCOME) categoryId = null
                        },
                        label = { Text(billTypeLabel(t)) },
                        leadingIcon = { FuriIcon(when (t) { BillType.SUBSCRIPTION -> "subscription"; BillType.INSTALMENT -> "instalment"; BillType.LOAN -> "loan"; BillType.INCOME -> "income"; else -> "bills" }, size = 18.dp) },
                    )
                }
            }
            OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.bill_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            app.furibudget.ui.settings.IconButtonRow(if (icon == "bills") BillEntity(name = "", type = type, accountId = 0, anchorDay = 1, firstDueEpochDay = 0, colorArgb = 0).iconKey() else icon, Color(typeColors.getValue(type))) { icon = it }
            if (!varies) AmountField(amountText, { amountText = it }, cur, stringResource(R.string.amount), Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.amount_varies_switch), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                Switch(varies, { varies = it; if (it) autoPay = false })
            }
            Text(stringResource(if (income) R.string.account else R.string.pay_from), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.accounts.forEach { a ->
                    FilterChip(selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text(a.name) }, leadingIcon = { FuriIcon(a.icon, size = 18.dp, tint = Color(a.colorArgb)) })
                }
            }
            Text(stringResource(R.string.category), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.filter { it.kind == (if (income) TxKind.INCOME else TxKind.EXPENSE) && !it.archived }.forEach { cat ->
                    FilterChip(selected = categoryId == cat.id, onClick = { categoryId = if (categoryId == cat.id) null else cat.id }, label = { Text(cat.name) }, leadingIcon = { FuriIcon(cat.icon, size = 18.dp, tint = Color(cat.colorArgb)) })
                }
            }
            FilterChip(selected = false, onClick = { pickingDue = true }, label = { Text(stringResource(R.string.first_due) + ": " + LocalDate.ofEpochDay(dueDay).format(dateFmt)) }, leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) })
            Text(stringResource(R.string.repeat), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { r -> FilterChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(repeatLabel(r)) }) }
            }
            if (repeat != Repeat.NONE) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { every = (every - 1).coerceAtLeast(1) }) { Text("−") }
                    val unit = stringResource(when (repeat) { Repeat.DAILY -> R.string.unit_days; Repeat.WEEKLY -> R.string.unit_weeks; Repeat.MONTHLY -> R.string.unit_months; else -> R.string.unit_years })
                    Text(stringResource(R.string.repeat_every_n, every) + " " + unit, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                    FilledTonalButton(onClick = { every = (every + 1).coerceAtMost(60) }) { Text("+") }
                }
                Text(stringResource(R.string.ends), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = endMode == 0, onClick = { endMode = 0 }, label = { Text(stringResource(R.string.end_never)) })
                    FilterChip(selected = endMode == 1, onClick = { endMode = 1 }, label = { Text(stringResource(R.string.end_after)) })
                    FilterChip(selected = endMode == 2, onClick = { endMode = 2 }, label = { Text(stringResource(R.string.end_on)) })
                }
                if (endMode == 1) OutlinedTextField(paymentsText, { paymentsText = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.payments_count)) }, singleLine = true, modifier = Modifier.width(220.dp))
                if (endMode == 2) FilterChip(selected = false, onClick = { pickingEnd = true }, label = { Text(LocalDate.ofEpochDay(endDay).format(dateFmt)) }, leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) })
            }
            Text(stringResource(R.string.remind_me), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-1, 0, 1, 3, 7).forEach { d ->
                    FilterChip(selected = remind == d, onClick = { remind = d }, label = { Text(when (d) { -1 -> stringResource(R.string.remind_off); 0 -> stringResource(R.string.remind_same_day); else -> stringResource(R.string.remind_days, d) }) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_pay_switch), style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                    Text(stringResource(R.string.auto_pay_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Switch(autoPay, { autoPay = it }, enabled = !varies)
            }
            OutlinedTextField(note, { note = it.take(80) }, label = { Text(stringResource(R.string.note)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (existing != null) History(existing, state)
        }
        Button(
            onClick = {
                val cat = state.category(categoryId)
                onSave(
                    BillEntity(
                        id = existing?.id ?: 0, name = name.trim(), kind = if (income) TxKind.INCOME else TxKind.EXPENSE, type = type,
                        amountMinor = amount?.minor ?: 0, accountId = accountId, toAccountId = existing?.toAccountId.takeIf { existing?.kind == TxKind.TRANSFER },
                        categoryId = categoryId, repeat = repeat, repeatEvery = if (repeat == Repeat.NONE) 1 else every,
                        anchorDay = LocalDate.ofEpochDay(dueDay).dayOfMonth, firstDueEpochDay = dueDay,
                        endEpochDay = if (repeat != Repeat.NONE && endMode == 2) endDay else null,
                        paymentsTotal = if (repeat != Repeat.NONE && endMode == 1) payments else null,
                        autoPay = autoPay && !varies, remindDaysBefore = remind, note = note.trim(),
                        icon = icon, colorArgb = cat?.colorArgb ?: typeColors.getValue(type), active = existing?.active ?: true,
                    ),
                )
                onClose()
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) { Text(stringResource(R.string.save)) }
    }

    if (pickingDue) DatePickerFor(dueDay, { dueDay = it }) { pickingDue = false }
    if (pickingEnd) DatePickerFor(endDay, { endDay = it }) { pickingEnd = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFor(day: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = day * 86_400_000L)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                st.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(st) }
}

/** Progress for instalments and the list of what was paid so far. */
@Composable
private fun History(bill: BillEntity, state: BudgetState) {
    val c = Furi.colors
    val mine = state.billPayments.filter { it.billId == bill.id }.sortedByDescending { it.dueEpochDay }
    if (bill.paymentsTotal != null) {
        val done = mine.size
        val last = BillSchedule.lastDue(Bills.spec(bill))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BudgetRing((done.toFloat() / bill.paymentsTotal).coerceIn(0f, 1f), Budgeting.Level.OK, Modifier.size(72.dp), stroke = 8.dp) {
                Text("$done/${bill.paymentsTotal}", style = MaterialTheme.typography.labelMedium, color = c.onSurface)
            }
            Column {
                Text(stringResource(R.string.payments_progress, done, bill.paymentsTotal), style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                if (last != null) Text(stringResource(R.string.payoff_on, last.format(dateFmt)), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
    Text(stringResource(R.string.payment_history), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
    if (mine.isEmpty()) Text(stringResource(R.string.no_payments_yet), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
    mine.take(12).forEach { p ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(LocalDate.ofEpochDay(p.dueEpochDay).format(dateFmt), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
            Text(
                if (p.status == PayStatus.SKIPPED) stringResource(R.string.skipped) else state.currency.format(Money(p.amountMinor), true),
                style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = if (p.status == PayStatus.SKIPPED) c.onSurfaceVariant else c.income,
            )
        }
    }
}
