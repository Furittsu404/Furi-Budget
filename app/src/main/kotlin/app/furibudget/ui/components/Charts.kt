package app.furibudget.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.furibudget.ui.theme.Furi
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

data class Slice(val label: String, val value: Long, val color: Color)

/** Drives 0 to 1 whenever [key] changes, so a chart replays its entrance when its data does. */
@Composable
private fun rememberEntrance(key: Any?, millis: Int = 900): Float {
    val motion = Furi.motion
    val p = remember { Animatable(0f) }
    LaunchedEffect(key, motion.enabled) {
        if (!motion.enabled) { p.snapTo(1f); return@LaunchedEffect }
        p.snapTo(0f)
        p.animateTo(1f, tween((millis * (1.2f - 0.2f * motion.scale)).toInt(), easing = FastOutSlowInEasing))
    }
    return p.value
}

/** Ring chart. Sweeps in, and a tapped slice steps out. [center] draws inside the hole. */
@Composable
fun DonutChart(
    slices: List<Slice>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
    center: @Composable () -> Unit,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val enter = rememberEntrance(slices.map { it.value })
    val c = Furi.colors
    val pop = remember { Animatable(0f) }
    val motion = Furi.motion
    LaunchedEffect(selected) { pop.animateTo(if (selected != null) 1f else 0f, motion.spring()) }
    Box(modifier.aspectRatio(1f).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(slices) {
                detectTapGestures { pos ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dist = hypot(pos.x - cx, pos.y - cy)
                    val outer = size.width / 2f
                    if (dist < outer * 0.5f || dist > outer) { onSelect(null); return@detectTapGestures }
                    var ang = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat() + 90f
                    if (ang < 0) ang += 360f
                    var acc = 0f
                    slices.forEachIndexed { i, s ->
                        val sweep = s.value.toFloat() / total * 360f
                        if (ang >= acc && ang < acc + sweep) onSelect(if (selected == i) null else i)
                        acc += sweep
                    }
                }
            },
        ) {
            val stroke = size.minDimension * 0.17f
            val pad = stroke * 0.55f
            val diameter = size.minDimension - pad * 2
            var start = -90f
            val gap = if (slices.size > 1) 2.5f else 0f
            slices.forEachIndexed { i, s ->
                val sweepFull = s.value.toFloat() / total * 360f
                val sweep = (sweepFull * enter - gap).coerceAtLeast(0.01f)
                val isSel = selected == i
                val grow = if (isSel) pop.value * stroke * 0.22f else 0f
                drawArc(
                    color = if (selected != null && !isSel) s.color.copy(alpha = 1f - 0.55f * pop.value) else s.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(pad - grow / 2f, pad - grow / 2f),
                    size = Size(diameter + grow, diameter + grow),
                    style = Stroke(stroke + grow),
                )
                start += sweepFull * enter
            }
            if (slices.isEmpty()) {
                drawArc(c.onSurface.copy(alpha = 0.1f), 0f, 360f, false, Offset(pad, pad), Size(diameter, diameter), style = Stroke(stroke))
            }
        }
        center()
    }
}

data class BarGroup(val label: String, val a: Long, val b: Long)

/** Paired bars (income, expense) that grow in one after another. [selected] gets full colour. */
@Composable
fun PairedBars(
    groups: List<BarGroup>,
    colorA: Color,
    colorB: Color,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    description: String = "",
) {
    val enter = rememberEntrance(groups.map { it.a to it.b })
    val maxV = max(1L, groups.maxOfOrNull { max(it.a, it.b) } ?: 1L).toFloat()
    val base = Furi.colors.onSurface.copy(alpha = 0.12f)
    Canvas(
        modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }.pointerInput(groups.size) {
            detectTapGestures { pos -> onSelect((pos.x / (size.width / groups.size)).toInt().coerceIn(0, groups.lastIndex)) }
        },
    ) {
        val slot = size.width / groups.size
        val barW = slot * 0.26f
        val gap = slot * 0.06f
        val floor = size.height
        drawLine(base, Offset(0f, floor), Offset(size.width, floor), strokeWidth = 2f)
        groups.forEachIndexed { i, g ->
            val stagger = ((enter * (groups.size + 1)) - i).coerceIn(0f, 1f)
            val alpha = if (i == selected) 1f else 0.5f
            val cx = slot * i + slot / 2f
            fun bar(v: Long, color: Color, x: Float) {
                val h = (v / maxV) * (size.height - 8f) * stagger
                drawRoundRect(color.copy(alpha = alpha), Offset(x, floor - h), Size(barW, h), androidx.compose.ui.geometry.CornerRadius(barW / 2.4f))
            }
            bar(g.a, colorA, cx - barW - gap / 2f)
            bar(g.b, colorB, cx + gap / 2f)
        }
    }
}

/**
 * Cumulative line with gradient fill, a dashed comparison line, and a drag scrubber that ticks the haptic motor per day.
 * [onScrub] reports the day index under the finger, or null when released.
 */
@Composable
fun TrendLine(
    values: List<Long>,
    previous: List<Long>,
    scrubIndex: Int?,
    onScrub: (Int?) -> Unit,
    lineBrush: Brush,
    fillColor: Color,
    modifier: Modifier = Modifier,
    description: String = "",
) {
    val enter = rememberEntrance(values)
    val c = Furi.colors
    val fb = app.furibudget.feedback.LocalFeedback.current
    val maxV = max(1L, max(values.maxOrNull() ?: 0L, previous.maxOrNull() ?: 0L)).toFloat()
    var lastTick by remember { mutableIntStateOf(-1) }
    Canvas(
        modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = description }.pointerInput(values.size) {
            fun idx(x: Float) = ((x / size.width) * (values.size - 1)).toInt().coerceIn(0, values.lastIndex)
            detectDragGestures(
                onDragStart = { o -> onScrub(idx(o.x)) },
                onDragEnd = { onScrub(null); lastTick = -1 },
                onDragCancel = { onScrub(null); lastTick = -1 },
                onDrag = { change, _ ->
                    change.consume()
                    val i = idx(change.position.x)
                    if (i != lastTick) { lastTick = i; fb.selection() }
                    onScrub(i)
                },
            )
        },
    ) {
        if (values.size < 2) return@Canvas
        val top = 12f
        val bottom = size.height - 4f
        fun x(i: Int) = size.width * i / (values.size - 1)
        fun y(v: Long) = bottom - (v / maxV) * (bottom - top)
        drawLine(c.onSurface.copy(alpha = 0.12f), Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = 2f)
        if (previous.size >= 2) {
            val pp = Path()
            previous.forEachIndexed { i, v ->
                val px = size.width * i / (previous.size - 1)
                if (i == 0) pp.moveTo(px, y(v)) else pp.lineTo(px, y(v))
            }
            drawPath(pp, c.onSurfaceVariant.copy(alpha = 0.45f), style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
        }
        val line = Path()
        values.forEachIndexed { i, v -> if (i == 0) line.moveTo(x(i), y(v)) else line.lineTo(x(i), y(v)) }
        val fill = Path().apply {
            addPath(line)
            lineTo(x(values.lastIndex), bottom); lineTo(0f, bottom); close()
        }
        clipRect(right = size.width * enter) {
            drawPath(fill, Brush.verticalGradient(listOf(fillColor.copy(alpha = 0.35f), Color.Transparent), startY = top, endY = bottom))
            drawPath(line, lineBrush, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (scrubIndex != null && scrubIndex in values.indices) {
            val sx = x(scrubIndex)
            drawLine(c.onSurface.copy(alpha = 0.35f), Offset(sx, top), Offset(sx, bottom), strokeWidth = 2f)
            drawCircle(c.background, 11f, Offset(sx, y(values[scrubIndex])))
            drawCircle(c.accent, 7f, Offset(sx, y(values[scrubIndex])))
        }
    }
}
