package app.furibudget.ui.edit

import app.furibudget.ui.theme.entityColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.R
import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.data.Budgeting
import app.furibudget.data.CategoryEntity
import app.furibudget.data.TagEntity
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.BudgetRing
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.theme.Furi

/** One row of a split: which category, and how much (as typed). */
data class SplitRow(val categoryId: Long?, val text: String)

/** Multi-select tag chips plus a chip that adds a new tag. */
@Composable
fun TagsRow(tags: List<TagEntity>, selected: Set<Long>, onToggle: (Long) -> Unit, onAdd: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.tags), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { t ->
                FilterChip(selected = t.id in selected, onClick = { onToggle(t.id) }, label = { Text(t.name) })
            }
            FilterChip(
                selected = false, onClick = { adding = true }, label = { Text(stringResource(R.string.new_tag)) },
                leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
            )
        }
    }
    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.new_tag)) },
            text = { OutlinedTextField(name, { name = it.take(24) }, singleLine = true, label = { Text(stringResource(R.string.tag_name)) }) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name.trim()); name = ""; adding = false }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * Split switch and rows. The ring closes as the rows add up to [total]; it turns red when they overshoot.
 * [rows] are owned by the caller so they survive rotation.
 */
@Composable
fun SplitEditor(
    on: Boolean, onToggle: (Boolean) -> Unit,
    rows: List<SplitRow>, onRows: (List<SplitRow>) -> Unit,
    total: Money, currency: Currency, categories: List<CategoryEntity>,
) {
    val c = Furi.colors
    val sum = rows.sumOf { currency.parse(it.text)?.minor ?: 0L }
    val left = total.minor - sum
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.split_across), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            Switch(on, onToggle)
        }
        AnimatedVisibility(on) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BudgetRing(
                        if (total.minor > 0) (sum.toFloat() / total.minor).coerceIn(0f, 1f) else 0f,
                        when { left < 0 -> Budgeting.Level.OVER; left == 0L && sum > 0 -> Budgeting.Level.OK; else -> Budgeting.Level.CLOSE },
                        Modifier.size(44.dp), stroke = 5.dp,
                    )
                    Text(
                        if (left >= 0) stringResource(R.string.split_left, currency.format(Money(left), true)) else stringResource(R.string.split_over, currency.format(Money(-left), true)),
                        style = MaterialTheme.typography.bodyMedium, color = if (left < 0) c.expense else c.onSurface,
                    )
                }
                rows.forEachIndexed { i, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = row.categoryId == cat.id,
                                    onClick = { onRows(rows.toMutableList().also { it[i] = row.copy(categoryId = cat.id) }) },
                                    label = { Text(cat.name) },
                                    leadingIcon = { FuriIcon(cat.icon, size = 16.dp, tint = entityColor(cat.colorArgb)) },
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AmountField(row.text, { t -> onRows(rows.toMutableList().also { it[i] = row.copy(text = t) }) }, currency, stringResource(R.string.amount), Modifier.weight(1f))
                            if (rows.size > 2) IconButton(onClick = { onRows(rows.toMutableList().also { it.removeAt(i) }) }) { Icon(Icons.Rounded.Close, stringResource(R.string.remove)) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onRows(rows + SplitRow(null, if (left > 0) currency.format(Money(left)).replace(",", "") else "")) }) { Text(stringResource(R.string.add_row)) }
                    if (left != 0L && rows.isNotEmpty()) TextButton(onClick = {
                        val last = rows.lastIndex
                        val before = rows.dropLast(1).sumOf { currency.parse(it.text)?.minor ?: 0L }
                        onRows(rows.toMutableList().also { it[last] = rows[last].copy(text = currency.format(Money((total.minor - before).coerceAtLeast(0))).replace(",", "")) })
                    }) { Text(stringResource(R.string.fill_last)) }
                }
            }
        }
    }
}
