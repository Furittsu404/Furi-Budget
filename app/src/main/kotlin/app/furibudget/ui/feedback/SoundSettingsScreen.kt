package app.furibudget.ui.feedback

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.BudgetViewModel
import app.furibudget.R
import app.furibudget.SoundCatalog
import app.furibudget.data.AppSettings
import app.furibudget.feedback.SoundPlayer
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import kotlinx.coroutines.delay

private val eventLabels = mapOf(
    "keypad" to R.string.ev_keypad, "saved" to R.string.ev_saved, "paid" to R.string.ev_paid, "deleted" to R.string.ev_deleted, "goal" to R.string.ev_goal,
)

@Composable
private fun styleLabel(style: String) = stringResource(
    when (style) {
        "soft" -> R.string.style_soft; "pop" -> R.string.style_pop; "coins" -> R.string.style_coins; "water" -> R.string.style_water
        "retro" -> R.string.style_retro; "silent" -> R.string.style_silent; else -> R.string.style_custom
    },
)

/** Switches, volume and strength, plus a style card per event with a play-on-tap preview and a custom file option. */
@Composable
fun SoundSettingsScreen(state: BudgetState, vm: BudgetViewModel, onBack: () -> Unit) {
    val c = Furi.colors
    val s = state.settings
    val ctx = LocalContext.current
    var pickFor by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<Int?>(null) }
    var customTick by remember { mutableIntStateOf(0) }
    val fb = app.furibudget.feedback.LocalFeedback.current
    var firstStrength by remember { mutableStateOf(true) }
    LaunchedEffect(s.hapticStrength) { if (firstStrength) firstStrength = false else fb.previewHaptic() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val event = pickFor ?: return@rememberLauncherForActivityResult
        if (uri != null) vm.importSound(uri, event) { err ->
            if (err == null) { vm.updateSettings { it.copy(soundStyles = it.soundStyles + (event to SoundPlayer.STYLE_CUSTOM)) }; customTick++; vm.previewSound(SoundPlayer.STYLE_CUSTOM, event); message = null }
            else message = when (err) { SoundPlayer.ImportError.TOO_BIG -> R.string.sound_too_big; SoundPlayer.ImportError.TOO_LONG -> R.string.sound_too_long; else -> R.string.sound_unreadable }
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.sound_haptics), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.sounds), style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                        Text(stringResource(R.string.sounds_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                    Switch(s.soundOn, { on -> vm.updateSettings { it.copy(soundOn = on) } })
                }
                if (s.soundOn) {
                    Text(stringResource(R.string.volume, s.soundVolume), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                    Slider(s.soundVolume.toFloat(), { v -> vm.updateSettings { it.copy(soundVolume = v.toInt()) } }, valueRange = 0f..100f)
                }
            }
        }
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.haptics), style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                        Text(stringResource(R.string.haptics_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                    Switch(s.hapticsOn, { on -> vm.updateSettings { it.copy(hapticsOn = on) } })
                }
                if (s.hapticsOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(R.string.strength_light, R.string.strength_normal, R.string.strength_strong).forEachIndexed { i, label ->
                            FilterChip(selected = s.hapticStrength == i, onClick = { vm.updateSettings { it.copy(hapticStrength = i) } }, label = { Text(stringResource(label)) })
                        }
                    }
                }
            }
        }
        if (message != null) Text(stringResource(message!!), style = MaterialTheme.typography.bodySmall, color = c.expense)
        SoundCatalog.events.forEach { event ->
            val current = s.soundStyles[event] ?: "soft"
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(eventLabels.getValue(event)), style = MaterialTheme.typography.titleSmall, color = c.onSurface)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (SoundCatalog.styles + listOf(SoundPlayer.STYLE_SILENT, SoundPlayer.STYLE_CUSTOM)).forEach { style ->
                        val hasFile = style != SoundPlayer.STYLE_CUSTOM || vm.hasCustomSound(event)
                        StyleCard(
                            style, selected = current == style && hasFile, enabled = true, tick = customTick,
                            onClick = {
                                if (style == SoundPlayer.STYLE_CUSTOM && !hasFile) { pickFor = event; picker.launch(arrayOf("audio/*")) }
                                else { vm.updateSettings { it.copy(soundStyles = it.soundStyles + (event to style)) }; vm.previewSound(style, event) }
                            },
                        )
                    }
                }
                if (current == SoundPlayer.STYLE_CUSTOM && vm.hasCustomSound(event)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { pickFor = event; picker.launch(arrayOf("audio/*")) }) { Text(stringResource(R.string.choose_another_file)) }
                        TextButton(onClick = { vm.clearSound(event); vm.updateSettings { it.copy(soundStyles = it.soundStyles + (event to "soft")) }; customTick++ }) { Text(stringResource(R.string.remove)) }
                    }
                }
            }
        }
        Text(stringResource(R.string.custom_sound_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

/** A tappable card: waveform bars that jump when played, and the style name underneath. */
@Composable
private fun StyleCard(style: String, selected: Boolean, enabled: Boolean, tick: Int, onClick: () -> Unit) {
    val c = Furi.colors
    val pulse = remember { Animatable(0f) }
    var played by remember { mutableIntStateOf(0) }
    val motionOn = Furi.motion.enabled
    LaunchedEffect(played) { if (played > 0 && motionOn) { pulse.snapTo(1f); pulse.animateTo(0f, tween(520)) } }
    val bars = remember(style) { val seed = style.hashCode(); List(7) { i -> 0.25f + ((seed * (i + 3) * 31 + i * 17) and 0xFF) / 255f * 0.7f } }
    val shape = MaterialTheme.shapes.medium
    val accent = c.accentText
    val muted = c.onSurfaceVariant
    Column(
        Modifier.width(84.dp).clip(shape)
            .background(if (selected) c.accentContainer else c.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.cardBorder, shape)
            .clickable(enabled = enabled) { played++; onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(width = 60.dp, height = 36.dp)) {
            val w = size.width / (bars.size * 2f - 1f)
            bars.forEachIndexed { i, h ->
                val boost = 1f + pulse.value * (if (i % 2 == 0) 0.6f else -0.35f)
                val silent = style == SoundPlayer.STYLE_SILENT
                val hh = (if (silent) 0.12f else h * boost).coerceIn(0.08f, 1f) * size.height
                drawRoundRect(if (selected) accent else muted, Offset(i * 2 * w, (size.height - hh) / 2), Size(w, hh), CornerRadius(w / 2))
            }
        }
        Text(styleLabel(style), style = MaterialTheme.typography.labelMedium, color = if (selected) c.onSurface else c.onSurfaceVariant, maxLines = 1)
    }
}
