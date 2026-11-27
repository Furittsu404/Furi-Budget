package app.furibudget.ui.bills

import app.furibudget.ui.theme.entityColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.BillOccurrence
import app.furibudget.data.TxKind
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.components.FuriIcons
import app.furibudget.ui.components.plain
import app.furibudget.ui.theme.Furi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/** Confirm sheet for Paid / Received: amount, account and date prefilled, one tap to record. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaySheet(state: BudgetState, occ: BillOccurrence, onConfirm: (amountMinor: Long, accountId: Long, paidOn: LocalDate) -> Unit, onDismiss: () -> Unit) {
    val cur = state.currency
    val bill = occ.bill
    val income = bill.kind == TxKind.INCOME
    var amount by remember { mutableStateOf(if (bill.amountMinor > 0) cur.plain(Money(bill.amountMinor)) else "") }
    var accountId by remember { mutableLongStateOf(state.accounts.firstOrNull { it.id == bill.accountId }?.id ?: state.accounts.firstOrNull()?.id ?: 0L) }
    var day by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var picking by remember { mutableStateOf(false) }
    val parsed = cur.parse(amount)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (income) R.string.confirm_receipt else R.string.confirm_payment), style = MaterialTheme.typography.titleLarge, color = Furi.colors.onSurface)
            Text(bill.name, style = MaterialTheme.typography.bodyLarge, color = Furi.colors.onSurfaceVariant)
            AmountField(amount, { amount = it }, cur, stringResource(R.string.amount), Modifier.fillMaxWidth())
            Text(stringResource(if (income) R.string.account else R.string.pay_from), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.accounts.forEach { a ->
                    FilterChip(
                        selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text(a.name) },
                        leadingIcon = { FuriIcon(a.icon, size = 18.dp, tint = entityColor(a.colorArgb)) },
                    )
                }
            }
            FilterChip(
                selected = false, onClick = { picking = true },
                label = { Text(LocalDate.ofEpochDay(day).format(dateFmt)) },
                leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) },
            )
            Button(
                onClick = { onConfirm(parsed!!.minor, accountId, LocalDate.ofEpochDay(day)) },
                enabled = parsed != null && parsed.minor > 0 && accountId != 0L,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.confirm)) }
        }
    }
    if (picking) {
        val st = rememberDatePickerState(initialSelectedDateMillis = day * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(st) }
    }
}
