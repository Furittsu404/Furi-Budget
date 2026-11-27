package app.furibudget.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.furibudget.ui.theme.Furi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A quiet empty state: a soft disc holding [icon], with three small dots drifting around it so the page feels
 * alive without asking for attention. Dots hold still when motion is off.
 */
@Composable
fun EmptyState(icon: ImageVector, title: String, hint: String, modifier: Modifier = Modifier, compact: Boolean = false, action: (@Composable () -> Unit)? = null) {
    val c = Furi.colors
    val turn by rememberInfiniteTransition(label = "empty").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "turn")
    val t = if (Furi.motion.enabled) turn else 0.1f
    val tint = c.accentText
    val disc = c.accentContainer.copy(alpha = 0.5f)
    Column(modifier.fillMaxWidth().padding(vertical = if (compact) 8.dp else 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val box = if (compact) 88.dp else 132.dp
        Box(Modifier.size(box), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(box)) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(disc, size.minDimension * 0.34f, center)
                listOf(0f to 5f, 0.33f to 4f, 0.66f to 3f).forEach { (phase, r) ->
                    val a = ((t + phase) % 1f) * 2f * PI.toFloat()
                    val rad = size.minDimension * 0.46f
                    drawCircle(tint.copy(alpha = 0.7f), r.dp.toPx(), Offset(center.x + cos(a) * rad, center.y + sin(a) * rad))
                }
            }
            Icon(icon, null, Modifier.size(if (compact) 30.dp else 44.dp), tint = tint)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.onSurface, textAlign = TextAlign.Center)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}
