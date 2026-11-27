package app.furibudget.ui.components

import app.furibudget.ui.theme.entityColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import app.furibudget.data.TxRole
import app.furibudget.ui.theme.Furi

/** One transaction line. Sits straight on the background, no card per row. */
@Composable
fun TxRow(tx: TransactionEntity, state: BudgetState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Furi.colors
    val category = state.category(tx.categoryId)
    val splitCount = remember(state.splits, tx.id) { state.splits.count { it.transactionId == tx.id } }
    val tagLine = remember(state.tags, state.transactionTags, tx.id) {
        val ids = state.transactionTags.filter { it.transactionId == tx.id }.map { it.tagId }
        state.tags.filter { it.id in ids }.joinToString(" ") { "#" + it.name }
    }
    val tint = if (tx.role != TxRole.NORMAL) c.accentText else if (tx.kind == TxKind.TRANSFER) c.accentText else entityColor(category?.colorArgb ?: 0xFF94A3B8.toInt())
    val person = remember(state.loans, state.people, tx.loanId) { tx.loanId?.let { id -> state.loans.firstOrNull { it.id == id } }?.let { l -> state.people.firstOrNull { it.id == l.personId } } }
    val title = if (tx.role == TxRole.GOAL) {
        stringResource(if (tx.kind == TxKind.EXPENSE) R.string.goal_saved_to else R.string.goal_taken_from, tx.note)
    } else if (tx.role == TxRole.ADJUSTMENT) {
        stringResource(R.string.balance_adjustment)
    } else if (person != null && tx.role == TxRole.LOAN) {
        stringResource(if (tx.kind == TxKind.EXPENSE) R.string.lent_to else R.string.borrowed_from, person.name)
    } else if (person != null && tx.role == TxRole.REPAYMENT) {
        if (tx.kind == TxKind.INCOME) stringResource(R.string.paid_back_by, person.name) else stringResource(R.string.you_paid, person.name)
    } else if (tx.role == TxRole.LOAN) {
        stringResource(if (tx.kind == TxKind.EXPENSE) R.string.loan_lent_generic else R.string.loan_borrowed_generic)
    } else if (tx.role == TxRole.REPAYMENT) {
        stringResource(R.string.loan_payment_generic)
    } else when (tx.kind) {
        TxKind.TRANSFER -> stringResource(R.string.transfer_to, state.account(tx.accountId)?.name ?: stringResource(R.string.unknown), state.account(tx.toAccountId)?.name ?: stringResource(R.string.unknown))
        else -> if (splitCount > 1) stringResource(R.string.split_title, splitCount) else category?.name ?: stringResource(R.string.uncategorised)
    }
    val original = if (tx.origCurrency != null && tx.origAmountMinor != null) {
        app.furibudget.core.Currency.all.firstOrNull { it.code == tx.origCurrency }?.let { it.format(Money(tx.origAmountMinor), true) } ?: ""
    } else ""
    val subtitle = listOf(original, if (tx.role == TxRole.GOAL) state.account(tx.accountId)?.name.orEmpty() else tx.note.ifBlank { if (tx.kind == TxKind.TRANSFER) stringResource(R.string.kind_transfer) else state.account(tx.accountId)?.name.orEmpty() }, tagLine).filter { it.isNotBlank() }.joinToString(" · ")
    val amount = state.currency.format(Money(tx.amountMinor), withSymbol = true)
    val (text, color) = when (tx.kind) {
        TxKind.INCOME -> "+ $amount" to c.income
        TxKind.EXPENSE -> "− $amount" to c.expense
        TxKind.TRANSFER -> amount to c.onSurfaceVariant
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            FuriIcon(if (tx.role == TxRole.GOAL) "flag" else if (person != null) person.icon else if (tx.kind == TxKind.TRANSFER) "transfer" else category?.icon ?: "other", tint = tint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text, style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = color)
    }
}
