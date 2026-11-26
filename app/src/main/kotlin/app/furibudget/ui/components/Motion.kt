package app.furibudget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.furibudget.R
import androidx.compose.ui.unit.dp
import app.furibudget.data.Budgeting
import app.furibudget.ui.theme.Furi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Progress ring that springs to its value and shifts colour as the limit nears. Over the limit it pulses. */
@Composable
fun BudgetRing(
    fraction: Float,
    level: Budgeting.Level,
    modifier: Modifier = Modifier,
    stroke: Dp = 14.dp,
    content: @Composable () -> Unit = {},
) {
    val c = Furi.colors
    val motion = Furi.motion
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(fraction) { sweep.animateTo(fraction.coerceIn(0f, 1f), motion.spring()) }
    val warn by animateColorAsState(c.warning, motion.tween(300), label = "warn")
    val over by animateColorAsState(c.expense, motion.tween(300), label = "over")
    // Only runs while the ring is over its limit and motion is on, so a calm screen costs no frames.
    val pulse = if (level == Budgeting.Level.OVER && motion.enabled) {
        rememberInfiniteTransition(label = "pulse").animateFloat(0.25f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse-a").value
    } else 0f
    val track = c.onSurface.copy(alpha = 0.12f)
    val ringDesc = stringResource(R.string.ring_desc, (fraction * 100).toInt())
    Box(modifier.semantics { contentDescription = ringDesc }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val d = size.minDimension - sw * 2.2f
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            val sweepDeg = sweep.value * 360f
            if (level == Budgeting.Level.OVER && motion.enabled) {
                drawArc(over.copy(alpha = pulse), 0f, 360f, false, tl, Size(d, d), style = Stroke(sw * 1.7f))
            }
            when (level) {
                Budgeting.Level.OK -> drawArc(
                    Brush.sweepGradient(listOf(c.accent, c.accent2, c.accent)), -90f, sweepDeg, false, tl, Size(d, d),
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
                Budgeting.Level.CLOSE -> drawArc(warn, -90f, sweepDeg, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                Budgeting.Level.OVER -> drawArc(over, -90f, 360f, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Rising liquid with two drifting waves. Fills with a spring; flat when motion is off. */
@Composable
fun LiquidFill(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val motion = Furi.motion
    val level = remember { Animatable(0f) }
    LaunchedEffect(fraction) { level.animateTo(fraction.coerceIn(0f, 1f), motion.spring()) }
    val phase = if (motion.enabled && fraction in 0.02f..0.98f) {
        rememberInfiniteTransition(label = "wave").animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "phase").value
    } else 0f
    Canvas(modifier) {
        val top = size.height * (1f - level.value)
        val amp = if (motion.enabled && level.value in 0.02f..0.98f) size.height * 0.035f else 0f
        fun wave(offset: Float, a: Float): Path {
            val p = Path()
            p.moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width + 4f) {
                p.lineTo(x, top + a * sin(x / size.width * 2f * PI.toFloat() + phase + offset))
                x += 4f
            }
            p.lineTo(size.width, size.height)
            p.close()
            return p
        }
        drawPath(wave(2f, amp * 0.8f), color.copy(alpha = 0.35f))
        drawPath(wave(0f, amp), Brush.verticalGradient(listOf(color.copy(alpha = 0.85f), color), startY = top, endY = size.height))
    }
}

/** One-shot confetti from the middle. Plays whenever [trigger] changes to a new non-zero value. */
@Composable
fun ConfettiBurst(trigger: Int, colors: List<Color>, modifier: Modifier = Modifier) {
    val t = remember { Animatable(1f) }
    val motion = Furi.motion
    LaunchedEffect(trigger) {
        if (trigger == 0 || !motion.enabled) return@LaunchedEffect
        t.snapTo(0f)
        t.animateTo(1f, tween(1600, easing = LinearEasing))
    }
    if (t.value >= 1f) return
    val parts = remember(trigger) {
        val r = Random(trigger)
        List(48) {
            val ang = r.nextFloat() * 2f * PI.toFloat()
            Triple(Offset(cos(ang), sin(ang) - 0.6f) * (0.5f + r.nextFloat()), r.nextFloat() * 360f, colors[r.nextInt(colors.size)])
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val o = Offset(size.width / 2f, size.height * 0.4f)
        val k = t.value
        parts.forEach { (v, rot, col) ->
            val pos = o + Offset(v.x * k * size.width * 0.55f, v.y * k * size.height * 0.4f + 0.9f * k * k * size.height * 0.5f)
            rotate(rot + k * 540f, pos) { drawRect(col.copy(alpha = (1f - k).coerceIn(0f, 1f)), pos - Offset(7f, 4f), Size(14f, 8f)) }
        }
    }
}
