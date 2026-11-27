package app.furibudget.ui.rates

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Currency
import app.furibudget.core.Rates
import app.furibudget.data.RateEntity
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

/** "2 h ago" style age for a cached rate. */
@Composable
fun ageLabel(at: Long): String {
    val mins = ((System.currentTimeMillis() - at) / 60_000L).coerceAtLeast(0)
    return when {
        mins < 1 -> stringResource(R.string.just_now)
        mins < 60 -> stringResource(R.string.ago_minutes, mins.toInt())
        mins < 60 * 24 -> stringResource(R.string.ago_hours, (mins / 60).toInt())
        else -> stringResource(R.string.ago_days, (mins / (60 * 24)).toInt())
    }
}

/** Every currency the app knows, what one unit is worth in your main currency, and where that number came from. */
@Composable
fun ExchangeRatesScreen(state: BudgetState, fetching: Boolean, onUpdate: () -> Unit, onSetManual: (String, Long) -> Unit, onClear: (String) -> Unit, onBack: () -> Unit) {
    val c = Furi.colors
    val base = state.currency
    val byCode = remember(state.rates) { state.rates.associateBy { it.code } }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.exchange_rates), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.settings.ratesOnline) {
                    Text(
                        if (state.settings.ratesFetchedAt > 0) stringResource(R.string.rates_updated, ageLabel(state.settings.ratesFetchedAt)) else stringResource(R.string.rates_never),
                        style = MaterialTheme.typography.bodyLarge, color = c.onSurface,
                    )
                    if (fetching) LinearProgressIndicator(Modifier.fillMaxWidth()) else FilledTonalButton(onClick = onUpdate) { Text(stringResource(R.string.update_now)) }
                } else {
                    Text(stringResource(R.string.rates_off_hint), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }
        }
        Currency.all.filter { it.code != base.code }.forEach { cur ->
            val r = byCode[cur.code]
            FuriCard(Modifier.fillMaxWidth().clickable { editing = cur.code }) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${cur.symbol} ${cur.code}", style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                        Text(
                            when { r == null -> stringResource(R.string.no_rate); r.manual -> stringResource(R.string.rate_manual); else -> stringResource(R.string.rate_online, ageLabel(r.fetchedAt)) },
                            style = MaterialTheme.typography.labelSmall, color = if (r?.manual == true) c.accentText else c.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (r == null) "—" else "1 ${cur.code} = ${base.symbol} ${Rates.display(r.perBaseMicro)}",
                        style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = c.onSurface,
                    )
                }
            }
        }
    }
    editing?.let { code -> RateDialog(code, base, byCode[code], onDismiss = { editing = null }, onSet = { onSetManual(code, it); editing = null }, onClear = { onClear(code); editing = null }) }
}

@Composable
private fun RateDialog(code: String, base: Currency, existing: RateEntity?, onDismiss: () -> Unit, onSet: (Long) -> Unit, onClear: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(existing?.let { Rates.format(it.perBaseMicro) }.orEmpty()) }
    val micro = Rates.parseTyped(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_rate_title, code)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.set_rate_hint, code, base.code), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    text, { text = it.take(14) }, singleLine = true, label = { Text("1 $code = ${base.symbol}") }, isError = text.isNotBlank() && micro == null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                )
                if (existing != null) TextButton(onClick = onClear) { Text(stringResource(if (existing.manual) R.string.use_online_rate else R.string.remove_rate)) }
            }
        },
        confirmButton = { TextButton(enabled = micro != null, onClick = { onSet(micro!!) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
