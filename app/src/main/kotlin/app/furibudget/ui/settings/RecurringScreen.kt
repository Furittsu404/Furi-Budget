package app.furibudget.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.core.Repeat
import app.furibudget.data.RecurrenceEntity
import app.furibudget.data.TxKind
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun repeatLabel(r: Repeat) = stringResource(
    when (r) {
        Repeat.NONE -> R.string.repeat_none
        Repeat.DAILY -> R.string.repeat_daily
        Repeat.WEEKLY -> R.string.repeat_weekly
        Repeat.MONTHLY -> R.string.repeat_monthly
        Repeat.YEARLY -> R.string.repeat_yearly
    },
)

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

@Composable
fun RecurringScreen(state: BudgetState, onSave: (RecurrenceEntity) -> Unit, onDelete: (RecurrenceEntity) -> Unit, onBack: () -> Unit) {
    val c = Furi.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.recurring), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }
        if (state.recurrences.isEmpty()) {
            Text(stringResource(R.string.no_recurring), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        state.recurrences.forEach { r ->
            val cat = state.category(r.categoryId)
            FuriCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            r.note.ifBlank { cat?.name ?: stringResource(if (r.kind == TxKind.TRANSFER) R.string.kind_transfer else R.string.uncategorised) },
                            style = MaterialTheme.typography.titleSmall, color = c.onSurface,
                        )
                        Text(
                            "${state.currency.format(Money(r.amountMinor), true)} · ${repeatLabel(r.repeat)}",
                            style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = if (r.kind == TxKind.INCOME) c.income else c.onSurfaceVariant,
                        )
                        Text(
                            if (r.active) stringResource(R.string.next_on, LocalDate.ofEpochDay(r.nextDueEpochDay).format(dateFmt)) else stringResource(R.string.paused),
                            style = MaterialTheme.typography.labelMedium, color = c.accentText,
                        )
                    }
                    Switch(checked = r.active, onCheckedChange = { onSave(r.copy(active = it)) })
                    IconButton(onClick = { onDelete(r) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = c.expense) }
                }
            }
        }
    }
}
