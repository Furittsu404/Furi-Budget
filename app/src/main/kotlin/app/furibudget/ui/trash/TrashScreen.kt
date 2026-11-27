package app.furibudget.ui.trash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.TRASH_DAYS
import app.furibudget.Trash
import app.furibudget.core.Money
import app.furibudget.data.BillEntity
import app.furibudget.data.Budgeting
import app.furibudget.data.FundEntity
import app.furibudget.data.GoalEntity
import app.furibudget.data.PersonEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import app.furibudget.ui.components.BudgetRing
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

@Composable
fun TrashScreen(
    state: BudgetState,
    trash: Trash,
    onRestoreFund: (FundEntity) -> Unit,
    onForeverFund: (FundEntity) -> Unit,
    onRestorePerson: (PersonEntity) -> Unit,
    onForeverPerson: (PersonEntity) -> Unit,
    onRestoreTx: (TransactionEntity) -> Unit,
    onRestoreBill: (BillEntity) -> Unit,
    onRestoreGoal: (GoalEntity) -> Unit,
    onForeverTx: (TransactionEntity) -> Unit,
    onForeverBill: (BillEntity) -> Unit,
    onForeverGoal: (GoalEntity) -> Unit,
    onBack: () -> Unit,
) {
    val c = Furi.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.recently_deleted), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }
        Text(stringResource(R.string.trash_hint, TRASH_DAYS.toInt()), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        if (trash.isEmpty) EmptyTrash()

        if (trash.funds.isNotEmpty()) Section(stringResource(R.string.funds))
        trash.funds.forEach { f -> Item(f.name, f.targetMinor?.let { state.currency.format(Money(it), true) } ?: stringResource(R.string.funds), f.deletedAt, { onRestoreFund(f) }, { onForeverFund(f) }) }
        if (trash.people.isNotEmpty()) Section(stringResource(R.string.trash_people))
        trash.people.forEach { p -> Item(p.name, p.note.ifBlank { stringResource(R.string.people) }, p.deletedAt, { onRestorePerson(p) }, { onForeverPerson(p) }) }
        if (trash.transactions.isNotEmpty()) Section(stringResource(R.string.trash_transactions))
        trash.transactions.forEach { tx ->
            val cat = state.category(tx.categoryId)
            val sign = when (tx.kind) { TxKind.INCOME -> "+ "; TxKind.EXPENSE -> "− "; else -> "" }
            Item(
                title = cat?.name ?: stringResource(R.string.uncategorised),
                detail = sign + state.currency.format(Money(tx.amountMinor), true) + if (tx.note.isNotBlank()) " · ${tx.note}" else "",
                deletedAt = tx.deletedAt,
                onRestore = { onRestoreTx(tx) }, onForever = { onForeverTx(tx) },
            )
        }
        if (trash.bills.isNotEmpty()) Section(stringResource(R.string.tab_bills))
        trash.bills.forEach { b ->
            Item(b.name, if (b.amountMinor > 0) state.currency.format(Money(b.amountMinor), true) else stringResource(R.string.amount_varies), b.deletedAt, { onRestoreBill(b) }, { onForeverBill(b) })
        }
        if (trash.goals.isNotEmpty()) Section(stringResource(R.string.goals_link))
        trash.goals.forEach { g ->
            Item(g.name, state.currency.format(Money(g.savedMinor), true) + " / " + state.currency.format(Money(g.targetMinor), true), g.deletedAt, { onRestoreGoal(g) }, { onForeverGoal(g) })
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = Furi.colors.onSurface, modifier = Modifier.padding(top = 6.dp))
}

/** One deleted thing: a ring counting down the days left, what it was, and Restore / Delete forever. */
@Composable
private fun Item(title: String, detail: String, deletedAt: Long?, onRestore: () -> Unit, onForever: () -> Unit) {
    val c = Furi.colors
    val daysLeft = (TRASH_DAYS - ((System.currentTimeMillis() - (deletedAt ?: 0L)) / DAY_MS)).coerceIn(0L, TRASH_DAYS)
    FuriCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BudgetRing(1f - daysLeft.toFloat() / TRASH_DAYS, if (daysLeft <= 5) Budgeting.Level.CLOSE else Budgeting.Level.OK, Modifier.size(52.dp), stroke = 6.dp) {
                Text("$daysLeft", style = MaterialTheme.typography.labelMedium, color = c.onSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = c.onSurface, maxLines = 1)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1)
                Text(stringResource(R.string.days_left, daysLeft.toInt()), style = MaterialTheme.typography.labelSmall, color = c.accentText)
            }
            FilledTonalButton(onClick = onRestore) { Text(stringResource(R.string.restore)) }
            IconButton(onClick = onForever) { Icon(Icons.Rounded.DeleteForever, stringResource(R.string.delete_forever), tint = c.expense) }
        }
    }
}

@Composable
private fun EmptyTrash() {
    val c = Furi.colors
    val breathe by rememberInfiniteTransition(label = "trash").animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "breathe")
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(120.dp).scale(if (Furi.motion.enabled) breathe else 1f).clip(CircleShape).background(c.accentContainer.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(56.dp), tint = c.accentText)
        }
        Text(stringResource(R.string.trash_empty), style = MaterialTheme.typography.titleMedium, color = c.onSurface, textAlign = TextAlign.Center)
        Text(stringResource(R.string.trash_empty_hint), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
