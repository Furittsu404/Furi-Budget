package app.furibudget.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.furibudget.core.Money
import app.furibudget.core.Periods
import app.furibudget.data.BudgetEntity
import app.furibudget.data.Budgeting
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.BudgetRing
import app.furibudget.ui.components.plain
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun levelColor(level: Budgeting.Level): Color = when (level) {
    Budgeting.Level.OK -> Furi.colors.accentText
    Budgeting.Level.CLOSE -> Furi.colors.warning
    Budgeting.Level.OVER -> Furi.colors.expense
}

@Composable
fun BudgetsScreen(state: BudgetState, onSave: (BudgetEntity) -> Unit, onDelete: (BudgetEntity) -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val today = LocalDate.now()
    val range = remember { Periods.monthContaining(today) }
    val overall = state.budgets.firstOrNull { it.categoryId == null }
    val perCategory = state.budgets.filter { it.categoryId != null }
    var editing by remember { mutableStateOf<BudgetEntity?>(null) }
    var creating by remember { mutableStateOf<Boolean?>(null) } // true = overall, false = category

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.tab_budgets), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, modifier = Modifier.padding(top = 8.dp))

        if (overall == null) {
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.set_monthly_budget), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Text(stringResource(R.string.monthly_budget_hint), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                    Button(onClick = { creating = true }) { Text(stringResource(R.string.set_budget)) }
                }
            }
        } else {
            val st = Budgeting.status(overall, state.transactions, range)
            val daysLeft = ChronoUnit.DAYS.between(today, range.end).toInt() + 1
            FuriCard(Modifier.fillMaxWidth().clickable { editing = overall }) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BudgetRing(st.fraction, st.level, Modifier.size(200.dp), stroke = 16.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(if (st.left.minor >= 0) R.string.left else R.string.over_by), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                            AnimatedAmount(cur.format(Money(kotlin.math.abs(st.left.minor))), Furi.figures.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize), levelColor(st.level))
                        }
                    }
                    Text(
                        stringResource(R.string.spent_of, cur.format(st.spent, true), cur.format(st.limit, true)),
                        style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                    )
                    if (st.left.minor > 0 && daysLeft > 0) {
                        Text(
                            stringResource(R.string.per_day, cur.format(Money(st.left.minor / daysLeft), true), daysLeft),
                            style = MaterialTheme.typography.labelLarge, color = c.accentText,
                        )
                    }
                }
            }
        }

        Text(stringResource(R.string.by_category), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        perCategory.forEach { b ->
            val cat = state.category(b.categoryId)
            val st = Budgeting.status(b, state.transactions, range)
            FuriCard(Modifier.fillMaxWidth().clickable { editing = b }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BudgetRing(st.fraction, st.level, Modifier.size(64.dp), stroke = 7.dp) {
                        Text("${(st.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = c.onSurface)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(cat?.name ?: "?", style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                        Text(
                            stringResource(R.string.spent_of, cur.format(st.spent), cur.format(st.limit)),
                            style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                        )
                    }
                    Text(
                        (if (st.left.minor < 0) "− " else "") + cur.format(Money(kotlin.math.abs(st.left.minor))),
                        style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = levelColor(st.level),
                    )
                }
            }
        }
        val free = state.categories.filter { cat -> cat.kind == app.furibudget.data.TxKind.EXPENSE && !cat.archived && perCategory.none { it.categoryId == cat.id } }
        if (free.isNotEmpty()) {
            FilledTonalButton(onClick = { creating = false }) { Text(stringResource(R.string.add_category_budget)) }
        }
    }

    val create = creating
    if (create != null || editing != null) {
        BudgetDialog(
            state = state,
            existing = editing,
            overall = create == true || editing?.categoryId == null,
            onDismiss = { creating = null; editing = null },
            onSave = { onSave(it); creating = null; editing = null },
            onDelete = { onDelete(it); editing = null },
        )
    }
}

@Composable
private fun BudgetDialog(
    state: BudgetState,
    existing: BudgetEntity?,
    overall: Boolean,
    onDismiss: () -> Unit,
    onSave: (BudgetEntity) -> Unit,
    onDelete: (BudgetEntity) -> Unit,
) {
    val cur = state.currency
    var text by remember { mutableStateOf(existing?.let { cur.plain(Money(it.limitMinor)) }.orEmpty()) }
    var rollover by remember { mutableStateOf(existing?.rollover ?: false) }
    val taken = state.budgets.mapNotNull { it.categoryId }.toSet()
    val choices = state.categories.filter { it.kind == app.furibudget.data.TxKind.EXPENSE && !it.archived && (it.id !in taken || it.id == existing?.categoryId) }
    var catId by remember { mutableStateOf(existing?.categoryId ?: choices.firstOrNull()?.id) }
    val amount = cur.parse(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (overall) R.string.monthly_budget else R.string.category_budget)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!overall && existing == null) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        choices.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { cat -> FilterChip(selected = catId == cat.id, onClick = { catId = cat.id }, label = { Text(cat.name) }) }
                            }
                        }
                    }
                }
                AmountField(text, { text = it }, cur, stringResource(R.string.limit), Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rollover), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(rollover, { rollover = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount.minor > 0 && (overall || catId != null),
                onClick = {
                    onSave(BudgetEntity(id = existing?.id ?: 0, categoryId = if (overall) null else catId, limitMinor = amount!!.minor, rollover = rollover))
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (existing != null) TextButton(onClick = { onDelete(existing) }) { Text(stringResource(R.string.delete), color = Furi.colors.expense) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
