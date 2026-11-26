package app.furibudget.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.Budgeting
import app.furibudget.data.GoalEntity
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.ConfettiBurst
import app.furibudget.ui.components.LiquidFill
import app.furibudget.ui.components.plain
import app.furibudget.ui.theme.Accents
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

@Composable
fun GoalsScreen(state: BudgetState, onSave: (GoalEntity) -> Unit, onDelete: (GoalEntity) -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val today = LocalDate.now()
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var topping by remember { mutableStateOf<GoalEntity?>(null) }
    var confetti by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_goals), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
                IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, stringResource(R.string.new_goal)) }
            }
            if (state.goals.isEmpty()) {
                FuriCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.no_goals_title), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                        Text(stringResource(R.string.no_goals_hint), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                        Button(onClick = { creating = true }) { Text(stringResource(R.string.new_goal)) }
                    }
                }
            }
            state.goals.forEach { g ->
                val plan = Budgeting.plan(g, today)
                val color = Color(g.colorArgb)
                val frac = if (g.targetMinor > 0) (g.savedMinor.toFloat() / g.targetMinor).coerceIn(0f, 1f) else 0f
                FuriCard(Modifier.fillMaxWidth().clickable { editing = g }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(96.dp).clip(CircleShape).background(c.onSurface.copy(alpha = 0.08f)).border(2.dp, color.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                            LiquidFill(frac, color, Modifier.fillMaxSize())
                            Text("${(frac * 100).toInt()}%", style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = c.onSurface)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                            Text(
                                stringResource(R.string.saved_of, cur.format(Money(g.savedMinor), true), cur.format(Money(g.targetMinor), true)),
                                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                            )
                            val line = when {
                                plan.reached -> stringResource(R.string.goal_reached)
                                plan.overdue -> stringResource(R.string.goal_overdue)
                                plan.monthlyNeeded != null -> stringResource(R.string.goal_monthly, cur.format(plan.monthlyNeeded, true), LocalDate.ofEpochDay(g.deadlineEpochDay!!).format(dateFmt))
                                else -> stringResource(R.string.goal_left, cur.format(plan.remaining, true))
                            }
                            Text(line, style = MaterialTheme.typography.labelMedium, color = if (plan.reached) c.income else if (plan.overdue) c.warning else c.accentText)
                            if (!plan.reached) FilledTonalButton(onClick = { topping = g }, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.add_money)) }
                        }
                    }
                }
            }
        }
        ConfettiBurst(confetti, listOf(c.accent, c.accent2, c.income, c.warning, c.expense))
    }

    if (creating || editing != null) {
        GoalDialog(
            state, editing,
            onDismiss = { creating = false; editing = null },
            onSave = { onSave(it); creating = false; editing = null },
            onDelete = { onDelete(it); editing = null },
        )
    }
    topping?.let { g ->
        MoneyDialog(
            state, g, onDismiss = { topping = null },
            onApply = { delta ->
                val next = (g.savedMinor + delta).coerceAtLeast(0)
                if (next >= g.targetMinor && g.savedMinor < g.targetMinor) confetti += 1
                onSave(g.copy(savedMinor = next))
                topping = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(state: BudgetState, existing: GoalEntity?, onDismiss: () -> Unit, onSave: (GoalEntity) -> Unit, onDelete: (GoalEntity) -> Unit) {
    val cur = state.currency
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var target by remember { mutableStateOf(existing?.let { cur.plain(Money(it.targetMinor)) }.orEmpty()) }
    var deadline by remember { mutableStateOf(existing?.deadlineEpochDay?.let { LocalDate.ofEpochDay(it) }) }
    var colorIdx by remember { mutableIntStateOf(existing?.let { g -> Accents.swatches.indexOfFirst { it.argb == g.colorArgb }.coerceAtLeast(0) } ?: (state.goals.size * 3) % Accents.swatches.size) }
    var picking by remember { mutableStateOf(false) }
    val amount = cur.parse(target)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.new_goal else R.string.edit_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.goal_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                AmountField(target, { target = it }, cur, stringResource(R.string.goal_target), Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = deadline != null, onClick = { picking = true }, label = { Text(deadline?.format(dateFmt) ?: stringResource(R.string.deadline)) })
                    if (deadline != null) TextButton(onClick = { deadline = null }) { Text(stringResource(R.string.clear)) }
                }
                Row(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Accents.swatches.forEachIndexed { i, sw ->
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Color(sw.argb))
                                .border(if (i == colorIdx) 3.dp else 0.dp, Furi.colors.onSurface, CircleShape)
                                .clickable { colorIdx = i },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && amount != null && amount.minor > 0,
                onClick = {
                    onSave(
                        (existing ?: GoalEntity(name = "", targetMinor = 0, colorArgb = 0)).copy(
                            name = name.trim(), targetMinor = amount!!.minor, deadlineEpochDay = deadline?.toEpochDay(), colorArgb = Accents.swatches[colorIdx].argb,
                        ),
                    )
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
    if (picking) {
        val st = rememberDatePickerState(initialSelectedDateMillis = (deadline ?: LocalDate.now().plusMonths(3)).toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { deadline = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(st) }
    }
}

@Composable
private fun MoneyDialog(state: BudgetState, goal: GoalEntity, onDismiss: () -> Unit, onApply: (Long) -> Unit) {
    val cur = state.currency
    var text by remember { mutableStateOf("") }
    val amount = cur.parse(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goal.name) },
        text = { AmountField(text, { text = it }, cur, stringResource(R.string.amount), Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = amount != null && amount.minor > 0, onClick = { onApply(amount!!.minor) }) { Text(stringResource(R.string.add_money)) } },
        dismissButton = {
            Row {
                TextButton(enabled = amount != null && amount.minor > 0, onClick = { onApply(-amount!!.minor) }) { Text(stringResource(R.string.withdraw)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
