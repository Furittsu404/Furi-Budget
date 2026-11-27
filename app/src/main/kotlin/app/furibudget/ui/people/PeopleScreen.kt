package app.furibudget.ui.people

import app.furibudget.ui.theme.entityColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.People
import app.furibudget.data.PersonBalance
import app.furibudget.data.PersonEntity
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.settings.ColorRow
import app.furibudget.ui.settings.IconButtonRow
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

/** Everyone you have lent to or borrowed from, with the totals on top. */
@Composable
fun PeopleScreen(state: BudgetState, onOpenPerson: (Long) -> Unit, onSavePerson: (PersonEntity, (Long) -> Unit) -> Unit, onBack: () -> Unit) {
    val c = Furi.colors
    val balances = remember(state.people, state.loans, state.transactions) { People.balances(state.people, state.loans, state.transactions) }
    val (owedToYou, youOwe) = remember(balances) { People.totals(balances) }
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.people), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            IconButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, stringResource(R.string.add_person)) }
        }
        if (balances.isEmpty()) {
            EmptyPeople { adding = true }
        } else {
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.owed_to_you), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                            AnimatedAmount(state.currency.symbol + " " + state.currency.format(Money(owedToYou)), Furi.figures.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), c.income)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.you_owe), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                            AnimatedAmount(state.currency.symbol + " " + state.currency.format(Money(youOwe)), Furi.figures.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), c.expense)
                        }
                    }
                    BalanceBar(owedToYou, youOwe)
                }
            }
            balances.sortedByDescending { kotlin.math.abs(it.netMinor) }.forEach { b -> PersonCard(b, state, onOpenPerson) }
        }
    }
    if (adding) PersonDialog(null, onDismiss = { adding = false }, onSave = { p -> onSavePerson(p) { id -> onOpenPerson(id) }; adding = false })
}

/** Green on the left for money owed to you, red on the right for money you owe; the split springs to its share. */
@Composable
fun BalanceBar(owedToYou: Long, youOwe: Long, modifier: Modifier = Modifier) {
    val c = Furi.colors
    val total = (owedToYou + youOwe).coerceAtLeast(1)
    val share by animateFloatAsState(if (owedToYou + youOwe == 0L) 0.5f else owedToYou.toFloat() / total, Furi.motion.spring(), label = "balance-share")
    Row(modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.onSurface.copy(alpha = 0.1f)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (owedToYou + youOwe == 0L) return@Row
        if (share > 0.001f) Box(Modifier.weight(share.coerceAtLeast(0.001f)).fillMaxSize().background(c.income))
        if (share < 0.999f) Box(Modifier.weight((1f - share).coerceAtLeast(0.001f)).fillMaxSize().background(c.expense))
    }
}

@Composable
private fun PersonCard(b: PersonBalance, state: BudgetState, onOpen: (Long) -> Unit) {
    val c = Furi.colors
    val tint = entityColor(b.person.colorArgb)
    val net = b.netMinor
    FuriCard(Modifier.fillMaxWidth().clickable { onOpen(b.person.id) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(b.person, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(b.person.name, style = MaterialTheme.typography.titleMedium, color = c.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(
                    when { net > 0 -> stringResource(R.string.owes_you); net < 0 -> stringResource(R.string.you_owe_them); else -> stringResource(R.string.all_settled) },
                    style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                )
            }
            if (net != 0L) Text(state.currency.format(Money(kotlin.math.abs(net)), true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = if (net > 0) c.income else c.expense)
            else Box(Modifier.size(10.dp).clip(CircleShape).background(tint.copy(alpha = 0.4f)))
        }
    }
}

@Composable
fun Avatar(p: PersonEntity, size: androidx.compose.ui.unit.Dp) {
    val tint = entityColor(p.colorArgb)
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        FuriIcon(p.icon, tint = tint, size = size * 0.55f)
    }
}

/** New person, or edit one. Icon and colour work like categories. */
@Composable
fun PersonDialog(existing: PersonEntity?, onDismiss: () -> Unit, onSave: (PersonEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "person") }
    var color by rememberSaveable { mutableStateOf(existing?.colorArgb ?: app.furibudget.ui.theme.Accents.swatches[3].argb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_person else R.string.edit_person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(30) }, singleLine = true, label = { Text(stringResource(R.string.person_name)) }, modifier = Modifier.fillMaxWidth())
                IconButtonRow(icon, Color(color)) { icon = it }
                ColorRow(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave((existing ?: PersonEntity(name = "", colorArgb = color)).copy(name = name.trim(), icon = icon, colorArgb = color)) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Two circles with arrows passing between them, drawn in the accent colour. */
@Composable
private fun EmptyPeople(onAdd: () -> Unit) {
    val c = Furi.colors
    val t by androidx.compose.animation.core.rememberInfiniteTransition(label = "people-art").animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2600), androidx.compose.animation.core.RepeatMode.Reverse), label = "t",
    )
    val shift = if (Furi.motion.enabled) t else 0.5f
    val accent = c.accentText; val a = c.income; val b = c.expense
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(Modifier.size(width = 220.dp, height = 120.dp)) {
            val r = size.height * 0.28f
            val cy = size.height / 2
            drawCircle(accent.copy(alpha = 0.22f), r, Offset(r + 8f, cy))
            drawCircle(accent.copy(alpha = 0.22f), r, Offset(size.width - r - 8f, cy))
            drawCircle(accent, r * 0.38f, Offset(r + 8f, cy - r * 0.18f)); drawCircle(accent, r * 0.38f, Offset(size.width - r - 8f, cy - r * 0.18f))
            val x0 = r * 2 + 28f; val x1 = size.width - r * 2 - 28f
            val sw = Stroke(5f, cap = StrokeCap.Round)
            drawLine(a, Offset(x0, cy - 12f), Offset(x1, cy - 12f), 5f, StrokeCap.Round)
            drawLine(b, Offset(x1, cy + 12f), Offset(x0, cy + 12f), 5f, StrokeCap.Round)
            val px = x0 + (x1 - x0) * shift
            drawCircle(a, 9f, Offset(px, cy - 12f)); drawCircle(b, 9f, Offset(x1 - (x1 - x0) * shift, cy + 12f))
        }
        Text(stringResource(R.string.people_empty), style = MaterialTheme.typography.titleMedium, color = c.onSurface, textAlign = TextAlign.Center)
        Text(stringResource(R.string.people_empty_hint), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        androidx.compose.material3.Button(onClick = onAdd) { Text(stringResource(R.string.add_person)) }
    }
}
