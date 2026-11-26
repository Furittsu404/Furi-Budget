package app.furibudget.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Periods
import app.furibudget.data.Ledger
import app.furibudget.data.TransactionEntity
import app.furibudget.ui.components.TxRow
import app.furibudget.ui.theme.Furi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

@Composable
fun ActivityScreen(state: BudgetState, onOpenTx: (Long) -> Unit, onDelete: (TransactionEntity) -> Unit) {
    val c = Furi.colors
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val range = remember(offset, state.startDay) { Periods.shiftMonth(state.month(), offset, state.startDay) }
    val inMonth = remember(state.transactions, range) { state.transactions.filter { LocalDate.ofEpochDay(it.epochDay) in range } }
    val grouped = remember(inMonth) { inMonth.groupBy { it.epochDay } }
    val totals = remember(state.transactions, range) { Ledger.totals(state.transactions, range) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.tab_activity), style = MaterialTheme.typography.headlineMedium, color = c.onSurface, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { offset -= 1 }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.prev_month)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(range.start.format(monthFmt), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("+ " + state.currency.format(totals.income), style = Furi.figures.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize), color = c.income)
                    Text("− " + state.currency.format(totals.expense), style = Furi.figures.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize), color = c.expense)
                }
            }
            IconButton(onClick = { offset += 1 }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month)) }
        }
        if (grouped.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_month), style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 132.dp)) {
                grouped.forEach { (day, list) ->
                    val date = LocalDate.ofEpochDay(day)
                    item(key = "h$day") {
                        Text(
                            when (date) {
                                today -> stringResource(R.string.today)
                                today.minusDays(1) -> stringResource(R.string.yesterday)
                                else -> date.format(dayFmt)
                            },
                            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelLarge, color = c.accentText,
                        )
                    }
                    items(list, key = { it.id }) { tx ->
                        SwipeRow(tx, state, onOpenTx, onDelete, Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.SwipeRow(
    tx: TransactionEntity,
    state: BudgetState,
    onOpen: (Long) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier,
) {
    val c = Furi.colors
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { v -> (v == SwipeToDismissBoxValue.EndToStart).also { if (it) onDelete(tx) } },
    )
    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier.animateItem(),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val p = dismiss.progress.coerceIn(0f, 1f)
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(c.expense.copy(alpha = 0.45f * p)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.delete), Modifier.padding(end = 20.dp).scale(0.8f + 0.4f * p), tint = c.expense)
            }
        },
    ) {
        TxRow(tx, state, Modifier.background(c.background.copy(alpha = 0f))) { onOpen(tx.id) }
    }
}
