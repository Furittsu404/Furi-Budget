package app.furibudget.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.furibudget.core.Money
import app.furibudget.data.Budgeting
import app.furibudget.data.Insights
import app.furibudget.ui.budgets.levelColor
import app.furibudget.ui.components.BudgetRing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Periods
import app.furibudget.data.Ledger
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.TxRow
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate

/** Interim Home (M2). M3 replaces it with the budget ring, category strip and goals. */
@Composable
fun HomeScreen(state: BudgetState, onOpenAppearance: () -> Unit, onOpenGoals: () -> Unit, onOpenSettings: () -> Unit, onOpenBudgets: () -> Unit, onOpenTx: (Long) -> Unit) {
    val c = Furi.colors
    val month = state.month()
    val totals = Ledger.totals(state.transactions, month)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 168.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
            IconButton(onClick = onOpenAppearance) {
                Icon(Icons.Rounded.Palette, contentDescription = stringResource(R.string.open_appearance))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.total_balance), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                if (state.loaded) {
                    AnimatedAmount(
                        state.currency.format(state.total, withSymbol = true),
                        Furi.figures.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                        c.onSurface,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("+ " + state.currency.format(totals.income, true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.income)
                    Text("− " + state.currency.format(totals.expense, true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.expense)
                }
                Text(stringResource(R.string.this_month), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            }
        }
        val overall = state.budgets.firstOrNull { it.categoryId == null }
        if (overall != null) {
            val st = Budgeting.status(overall, state.transactions, Budgeting.rangeFor(overall, LocalDate.now(), state.startDay, state.firstDay), state.startDay)
            FuriCard(Modifier.fillMaxWidth().clickable(onClick = onOpenBudgets)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    BudgetRing(st.fraction, st.level, Modifier.size(92.dp), stroke = 9.dp) {
                        Text("${(st.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = c.onSurface)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(if (st.left.minor >= 0) R.string.left_this_month else R.string.over_this_month), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                        Text(
                            state.currency.format(Money(kotlin.math.abs(st.left.minor)), true),
                            style = Furi.figures.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = levelColor(st.level),
                        )
                        Text(stringResource(R.string.spent_of, state.currency.format(st.spent), state.currency.format(st.limit)), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                }
            }
        }
        FuriCard(Modifier.fillMaxWidth().clickable(onClick = onOpenGoals)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.goals_link), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                Text(
                    if (state.goals.isEmpty()) stringResource(R.string.no_goals_title) else state.goals.size.toString(),
                    style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                )
            }
        }
        val top = Insights.spendByCategory(state.transactions, month).take(3)
        if (top.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.top_spending), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                val biggest = top.first().amount.minor.coerceAtLeast(1)
                top.forEach { ct ->
                    val cat = state.category(ct.categoryId)
                    TopBar(cat?.name ?: stringResource(R.string.uncategorised), Color(cat?.colorArgb ?: 0xFF94A3B8.toInt()), ct.amount.minor.toFloat() / biggest, state.currency.format(ct.amount, true))
                }
            }
        }
        Column {
            Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.padding(bottom = 4.dp))
            if (state.transactions.isEmpty()) {
                Text(stringResource(R.string.empty_activity), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            }
            state.transactions.take(5).forEach { tx -> TxRow(tx, state) { onOpenTx(tx.id) } }
        }
    }
}

/** One horizontal bar that grows in on first show. */
@Composable
private fun TopBar(label: String, color: Color, fraction: Float, amount: String) {
    val c = Furi.colors
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val f by animateFloatAsState(if (go) fraction else 0f, Furi.motion.spring(), label = "top-bar")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
            Text(amount, style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = c.onSurface)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.onSurface.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxWidth(f.coerceIn(0.02f, 1f)).height(10.dp).clip(CircleShape).background(color))
        }
    }
}
