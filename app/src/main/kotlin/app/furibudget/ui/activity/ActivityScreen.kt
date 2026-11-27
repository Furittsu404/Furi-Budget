package app.furibudget.ui.activity

import androidx.compose.material.icons.rounded.Receipt
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import app.furibudget.data.TxKind
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBoxState
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
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var kindFilter by rememberSaveable { mutableStateOf<TxKind?>(null) }
    val inMonth = remember(state.transactions, state.transactionTags, state.tags, range, query, kindFilter, searching) {
        val q = if (searching) query.trim().lowercase() else ""
        val tagNames = state.tags.associate { it.id to it.name.lowercase() }
        val tagsOf = state.transactionTags.groupBy({ it.transactionId }, { tagNames[it.tagId].orEmpty() })
        state.transactions.filter { t ->
            LocalDate.ofEpochDay(t.epochDay) in range && (!searching || kindFilter == null || t.kind == kindFilter) &&
                (q.isEmpty() || listOf(t.note, state.category(t.categoryId)?.name.orEmpty(), state.account(t.accountId)?.name.orEmpty(), state.currency.format(app.furibudget.core.Money(t.amountMinor))) .plus(tagsOf[t.id].orEmpty())
                    .any { it.lowercase().contains(q) })
        }
    }
    val grouped = remember(inMonth) { inMonth.groupBy { it.epochDay } }
    val totals = remember(state.transactions, range) { Ledger.totals(state.transactions, range) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_activity), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
            IconButton(onClick = { searching = !searching; if (!searching) { query = ""; kindFilter = null } }) {
                Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, stringResource(R.string.search))
            }
        }
        if (searching) {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kindFilter == null, onClick = { kindFilter = null }, label = { Text(stringResource(R.string.filter_all)) })
                listOf(TxKind.EXPENSE to R.string.kind_expense, TxKind.INCOME to R.string.kind_income, TxKind.TRANSFER to R.string.kind_transfer).forEach { (k, l) ->
                    FilterChip(selected = kindFilter == k, onClick = { kindFilter = if (kindFilter == k) null else k }, label = { Text(stringResource(l)) })
                }
            }
        }
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
                app.furibudget.ui.components.EmptyState(
                    if (searching) androidx.compose.material.icons.Icons.Rounded.Search else androidx.compose.material.icons.Icons.Rounded.Receipt,
                    stringResource(if (searching) R.string.no_matches else R.string.activity_empty_title),
                    if (searching) "" else stringResource(R.string.empty_month),
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 168.dp)) {
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
    val deleteLabel = stringResource(R.string.delete)
    // The money handed out for a loan is removed by deleting the person, not by swiping it away on its own.
    val deletable = tx.role != app.furibudget.data.TxRole.LOAN
    // Not the saveable variant: that one keeps "already swiped away" for a row id, so a row brought back by
    // Undo came back stuck and the next swipe opened the editor instead. A fresh state per row entry fixes that.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val deleteNow by androidx.compose.runtime.rememberUpdatedState(onDelete)
    val dismiss = remember(tx.id) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = density,
            confirmValueChange = { v -> (v == SwipeToDismissBoxValue.EndToStart).also { if (it) deleteNow(tx) } },
            positionalThreshold = { total -> total * 0.5f },
        )
    }
    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier.animateItem().semantics {
            // Swipe is not reachable with a screen reader, so offer the same action by name.
            if (deletable) customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(tx); true })
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = deletable,
        backgroundContent = {
            // progress reads 1 while the row sits still, so only draw the red layer once a swipe is under way.
            val swiping = dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val p = if (swiping) dismiss.progress.coerceIn(0f, 1f) else 0f
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(c.expense.copy(alpha = 0.45f * p)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (swiping) Icon(Icons.Rounded.Delete, stringResource(R.string.delete), Modifier.padding(end = 20.dp).scale(0.8f + 0.4f * p), tint = c.expense)
            }
        },
    ) {
        TxRow(tx, state, Modifier.background(c.background.copy(alpha = 0f))) { onOpen(tx.id) }
    }
}
