package app.furibudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.furibudget.ui.theme.Furi

/** Digit pad for amounts. Keys dip on press and tick the haptic motor; long-press on backspace clears. */
@Composable
fun Keypad(
    showPoint: Boolean,
    onDigit: (Int) -> Unit,
    onPoint: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d -> Key(Modifier.weight(1f), onClick = { onDigit(d) }) { Text("$d", style = Furi.figures, color = Furi.colors.onSurface) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showPoint) {
                Key(Modifier.weight(1f), onClick = onPoint) { Text(".", style = Furi.figures, color = Furi.colors.onSurface) }
            } else {
                Box(Modifier.weight(1f))
            }
            Key(Modifier.weight(1f), onClick = { onDigit(0) }) { Text("0", style = Furi.figures, color = Furi.colors.onSurface) }
            Key(Modifier.weight(1f), onClick = onBackspace, onLongClick = onClear, label = "Backspace") {
                Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = Furi.colors.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val c = Furi.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, Furi.motion.spring(), label = "key")
    val haptic = LocalHapticFeedback.current
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier
            .height(if (LocalDensity.current.fontScale > 1.15f) 46.dp else 56.dp)
            .scale(scale)
            .clip(shape)
            .background(if (pressed) c.accentContainer else c.card)
            .border(1.dp, c.cardBorder.copy(alpha = c.cardBorder.alpha * 0.6f), shape)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
                onLongClick = onLongClick?.let { { haptic.performHapticFeedback(HapticFeedbackType.LongPress); it() } },
            )
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}
