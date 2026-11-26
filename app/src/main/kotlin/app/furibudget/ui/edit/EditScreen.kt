package app.furibudget.ui.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.KeypadEntry
import app.furibudget.core.Money
import app.furibudget.core.Recurrence
import app.furibudget.core.Repeat
import app.furibudget.data.RecurrenceEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.ui.settings.repeatLabel
import app.furibudget.data.TxKind
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.FuriIcons
import app.furibudget.ui.components.Keypad
import app.furibudget.ui.theme.Furi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    txId: Long,
    state: BudgetState,
    onSave: (TransactionEntity) -> Unit,
    onSaveRecurrence: (RecurrenceEntity) -> Unit,
    onClose: () -> Unit,
) {
    val c = Furi.colors
    val existing = remember(txId, state.loaded) { state.transactions.firstOrNull { it.id == txId } }
    val decimals = state.currency.decimals

    var kind by remember(existing) { mutableStateOf(existing?.kind ?: TxKind.EXPENSE) }
    var entry by remember(existing) { mutableStateOf(existing?.let { KeypadEntry.from(Money(it.amountMinor), decimals) } ?: KeypadEntry(decimals = decimals)) }
    var accountId by remember(existing, state.accounts) { mutableLongStateOf(existing?.accountId ?: state.accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember(existing) { mutableStateOf(existing?.toAccountId) }
    var categoryId by remember(existing) { mutableStateOf(existing?.categoryId) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var date by remember(existing) { mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.epochDay) } ?: LocalDate.now()) }
    var picking by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(Repeat.NONE) }

    val amount = entry.toMoney()
    val kindColor by animateColorAsState(
        when (kind) { TxKind.EXPENSE -> c.expense; TxKind.INCOME -> c.income; TxKind.TRANSFER -> c.accentText },
        Furi.motion.tween(240), label = "kind-color",
    )
    val transferTarget = toAccountId ?: state.accounts.firstOrNull { it.id != accountId }?.id
    val canSave = amount.minor > 0 && accountId != 0L && (kind != TxKind.TRANSFER || (transferTarget != null && transferTarget != accountId))

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            Text(
                stringResource(if (existing == null) R.string.new_transaction else R.string.edit_transaction),
                style = MaterialTheme.typography.titleLarge, color = c.onSurface,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val kinds = listOf(TxKind.EXPENSE to R.string.kind_expense, TxKind.INCOME to R.string.kind_income, TxKind.TRANSFER to R.string.kind_transfer)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { i, (k, label) ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = {
                            kind = k
                            if (state.category(categoryId)?.kind != k) categoryId = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, kinds.size),
                    ) { Text(stringResource(label)) }
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                AnimatedAmount(
                    text = state.currency.symbol + " " + entry.display(),
                    style = Furi.figures.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize),
                    color = kindColor,
                    rollFromZero = false,
                )
            }
            if (kind != TxKind.TRANSFER) {
                ChipRow(stringResource(R.string.category)) {
                    state.categories.filter { it.kind == kind && !it.archived }.forEach { cat ->
                        val col = Color(cat.colorArgb)
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = if (categoryId == cat.id) null else cat.id },
                            label = { Text(cat.name) },
                            leadingIcon = { Icon(FuriIcons.byKey(cat.icon), null, Modifier.size(18.dp), tint = col) },
                        )
                    }
                }
            }
            ChipRow(stringResource(if (kind == TxKind.TRANSFER) R.string.from_account else R.string.account)) {
                state.accounts.forEach { acc ->
                    FilterChip(
                        selected = accountId == acc.id,
                        onClick = { accountId = acc.id },
                        label = { Text(acc.name) },
                        leadingIcon = { Icon(FuriIcons.byKey(acc.icon), null, Modifier.size(18.dp), tint = Color(acc.colorArgb)) },
                    )
                }
            }
            if (kind == TxKind.TRANSFER) {
                ChipRow(stringResource(R.string.to_account)) {
                    state.accounts.filter { it.id != accountId }.forEach { acc ->
                        FilterChip(
                            selected = transferTarget == acc.id,
                            onClick = { toAccountId = acc.id },
                            label = { Text(acc.name) },
                            leadingIcon = { Icon(FuriIcons.byKey(acc.icon), null, Modifier.size(18.dp), tint = Color(acc.colorArgb)) },
                        )
                    }
                }
            }
            if (existing == null) {
                ChipRow(stringResource(R.string.repeat)) {
                    Repeat.entries.forEach { r -> FilterChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(repeatLabel(r)) }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = false,
                    onClick = { picking = true },
                    label = { Text(dateLabel(date)) },
                    leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(80) },
                    placeholder = { Text(stringResource(R.string.note)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Column(Modifier.padding(top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Keypad(
                showPoint = decimals > 0,
                onDigit = { entry = entry.digit(it) },
                onPoint = { entry = entry.point() },
                onBackspace = { entry = entry.backspace() },
                onClear = { entry = KeypadEntry(decimals = decimals) },
            )
            Button(
                onClick = {
                    onSave(
                        TransactionEntity(
                            id = existing?.id ?: 0,
                            kind = kind,
                            amountMinor = amount.minor,
                            accountId = accountId,
                            toAccountId = if (kind == TxKind.TRANSFER) transferTarget else null,
                            categoryId = if (kind == TxKind.TRANSFER) null else categoryId,
                            note = note.trim(),
                            epochDay = date.toEpochDay(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                    if (repeat != Repeat.NONE && existing == null) {
                        onSaveRecurrence(
                            RecurrenceEntity(
                                kind = kind, amountMinor = amount.minor, accountId = accountId,
                                toAccountId = if (kind == TxKind.TRANSFER) transferTarget else null,
                                categoryId = if (kind == TxKind.TRANSFER) null else categoryId, note = note.trim(),
                                repeat = repeat, anchorDay = date.dayOfMonth,
                                nextDueEpochDay = Recurrence.next(date, repeat, date.dayOfMonth).toEpochDay(),
                            ),
                        )
                    }
                    onClose()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (picking) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
