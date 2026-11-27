package app.furibudget.ui.bills

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.furibudget.R
import app.furibudget.core.BillStatus
import app.furibudget.data.BillEntity
import app.furibudget.data.BillOccurrence
import app.furibudget.data.BillType
import app.furibudget.data.TxKind
import app.furibudget.ui.theme.Furi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Calendar dot and chip colour for a status. */
@Composable
fun statusColor(status: BillStatus): Color = when (status) {
    BillStatus.OVERDUE -> Furi.colors.expense
    BillStatus.DUE_TODAY, BillStatus.DUE_SOON -> Furi.colors.warning
    BillStatus.UPCOMING -> Furi.colors.accentText
    BillStatus.PAID -> Furi.colors.income
    BillStatus.SKIPPED -> Furi.colors.onSurfaceVariant
}

/** "Due in 3 days", "Overdue since yesterday", "Paid on Nov 3" and so on. */
@Composable
fun dueLabel(occ: BillOccurrence, today: LocalDate, dateText: (LocalDate) -> String): String {
    val days = ChronoUnit.DAYS.between(today, occ.due).toInt()
    return when (occ.status) {
        BillStatus.PAID -> stringResource(if (occ.bill.kind == TxKind.INCOME) R.string.received_on else R.string.paid_on, dateText(LocalDate.ofEpochDay(occ.payment?.paidEpochDay ?: occ.due.toEpochDay())))
        BillStatus.SKIPPED -> stringResource(R.string.skipped)
        BillStatus.OVERDUE -> if (days == -1) stringResource(R.string.overdue_yesterday) else stringResource(R.string.overdue_days, -days)
        BillStatus.DUE_TODAY -> stringResource(R.string.due_today)
        else -> if (days == 1) stringResource(R.string.due_tomorrow) else stringResource(R.string.due_in_days, days)
    }
}

/** The icon the user picked for this bill, or one that matches its type when they did not. */
fun BillEntity.iconKey(): String = if (icon != "bills" && icon.isNotBlank()) icon else when (type) {
    BillType.SUBSCRIPTION -> "subscription"
    BillType.INSTALMENT -> "instalment"
    BillType.LOAN -> "loan"
    BillType.INCOME -> "income"
    else -> icon
}

@Composable
fun billTypeLabel(t: BillType) = stringResource(
    when (t) {
        BillType.BILL -> R.string.type_bill
        BillType.SUBSCRIPTION -> R.string.type_subscription
        BillType.INSTALMENT -> R.string.type_instalment
        BillType.LOAN -> R.string.type_loan
        BillType.INCOME -> R.string.type_income
        BillType.RECURRING -> R.string.type_recurring
    },
)
