package app.furibudget.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.core.Money
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import app.furibudget.ui.theme.Furi

/** One transaction line. Sits straight on the background, no card per row. */
@Composable
fun TxRow(tx: TransactionEntity, state: BudgetState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Furi.colors
    val category = state.category(tx.categoryId)
    val tint = if (tx.kind == TxKind.TRANSFER) c.accentText else Color(category?.colorArgb ?: 0xFF94A3B8.toInt())
    val title = when (tx.kind) {
        TxKind.TRANSFER -> "${state.account(tx.accountId)?.name ?: "?"} → ${state.account(tx.toAccountId)?.name ?: "?"}"
        else -> category?.name ?: "Uncategorised"
    }
    val subtitle = tx.note.ifBlank { if (tx.kind == TxKind.TRANSFER) "Transfer" else state.account(tx.accountId)?.name.orEmpty() }
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
            Icon(FuriIcons.byKey(if (tx.kind == TxKind.TRANSFER) "transfer" else category?.icon ?: "other"), null, tint = tint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text, style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = color)
    }
}
