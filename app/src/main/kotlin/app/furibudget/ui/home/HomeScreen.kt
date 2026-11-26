package app.furibudget.ui.home

import androidx.compose.foundation.layout.Arrangement
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
fun HomeScreen(state: BudgetState, onOpenAppearance: () -> Unit, onOpenTx: (Long) -> Unit) {
    val c = Furi.colors
    val month = Periods.monthContaining(LocalDate.now())
    val totals = Ledger.totals(state.transactions, month)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
            IconButton(onClick = onOpenAppearance) {
                Icon(Icons.Rounded.Palette, contentDescription = stringResource(R.string.open_appearance))
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
        Column {
            Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.padding(bottom = 4.dp))
            if (state.transactions.isEmpty()) {
                Text(stringResource(R.string.empty_activity), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            }
            state.transactions.take(5).forEach { tx -> TxRow(tx, state) { onOpenTx(tx.id) } }
        }
    }
}
