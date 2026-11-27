package app.furibudget.ui.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Full-screen background behind the navigation host. Drifts slowly unless motion is off. */
@Composable
fun FuriBackground(modifier: Modifier = Modifier) {
    val c = Furi.colors
    val kind = Furi.spec.background ?: if (Furi.spec.minimal) BackgroundKind.SOLID else Furi.pack.defaultBackground
    val motion = Furi.motion
    val scale by rememberUpdatedState(motion.scale)
    val clock = remember { mutableFloatStateOf(0f) }
    val animating = kind in setOf(BackgroundKind.MESH, BackgroundKind.AURORA, BackgroundKind.PARTICLES) && motion.enabled
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) clock.floatValue += (now - last) / 1e9f * scale
                last = now
            }
        }
    }
    val tilt = if (Furi.spec.parallax && motion.enabled) rememberTilt() else null
    val shift = Modifier.graphicsLayer {
        val t = tilt?.value
        if (t != null) {
            translationX = t.x * 28.dp.toPx()
            translationY = t.y * 28.dp.toPx()
            scaleX = 1.12f
            scaleY = 1.12f
        }
    }
    Box(modifier.fillMaxSize().background(c.background)) {
        when {
            kind == BackgroundKind.SOLID -> Unit
            kind == BackgroundKind.PHOTO && Furi.spec.photoPath != null -> PhotoLayer(Furi.spec, shift, c.background)
            kind == BackgroundKind.PARTICLES -> Canvas(Modifier.fillMaxSize().then(shift)) {
                drawBlobs(c, 0f, strength = 0.6f)
                drawParticles(c, clock.floatValue)
            }
            (kind == BackgroundKind.MESH || kind == BackgroundKind.AURORA) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                ShaderLayer(Modifier.then(shift), c, clock, aurora = kind == BackgroundKind.AURORA)
            else -> Canvas(Modifier.fillMaxSize().then(shift)) {
                drawBlobs(c, if (kind == BackgroundKind.GRADIENT) 0f else clock.floatValue)
            }
        }
    }
}

private fun Modifier.background(color: Color) = this.then(Modifier.drawWithCache { onDrawBehind { drawRect(color) } })

private fun DrawScope.drawBlobs(c: FuriColors, t: Float, strength: Float = 1f) {
    drawRect(c.background)
    val w = size.width
    val h = size.height
    val r = max(w, h) * 0.75f
    fun blob(color: Color, cx: Float, cy: Float) = drawRect(
        Brush.radialGradient(listOf(color.copy(alpha = color.alpha * strength), Color.Transparent), center = Offset(cx, cy), radius = r),
    )
    blob(c.meshA, w * (0.25f + 0.12f * sin(t * 0.21f)), h * (0.18f + 0.08f * cos(t * 0.17f)))
    blob(c.meshB, w * (0.85f + 0.10f * cos(t * 0.19f)), h * (0.50f + 0.12f * sin(t * 0.23f)))
    blob(c.meshC, w * (0.30f + 0.14f * sin(t * 0.15f + 2f)), h * (0.88f + 0.06f * cos(t * 0.25f)))
}

private const val MESH_AGSL = """
uniform float2 size;
uniform float time;
uniform float4 bg;
uniform float4 ca;
uniform float4 cb;
uniform float4 cc;

float blob(float2 p, float2 center, float radius) {
    float w = clamp(1.0 - distance(p, center) / radius, 0.0, 1.0);
    return w * w * (3.0 - 2.0 * w);
}

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 fc) {
    float2 uv = fc / size;
    float asp = size.x / size.y;
    float2 p = float2(uv.x * asp, uv.y);
    float2 pa = float2((0.25 + 0.12 * sin(time * 0.21)) * asp, 0.18 + 0.08 * cos(time * 0.17));
    float2 pb = float2((0.85 + 0.10 * cos(time * 0.19)) * asp, 0.50 + 0.12 * sin(time * 0.23));
    float2 pc = float2((0.30 + 0.14 * sin(time * 0.15 + 2.0)) * asp, 0.88 + 0.06 * cos(time * 0.25));
    float3 col = bg.rgb;
    col = mix(col, ca.rgb, ca.a * blob(p, pa, 0.95));
    col = mix(col, cb.rgb, cb.a * blob(p, pb, 0.95));
    col = mix(col, cc.rgb, cc.a * blob(p, pc, 0.95));
    col += (hash(fc) - 0.5) / 255.0 * 1.5;
    return half4(col, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderLayer(modifier: Modifier, c: FuriColors, clock: androidx.compose.runtime.MutableFloatState, aurora: Boolean) {
    val shader = remember(aurora) { RuntimeShader(if (aurora) AURORA_AGSL else MESH_AGSL) }
    Box(
        modifier.fillMaxSize().drawWithCache {
            val brush = ShaderBrush(shader)
            onDrawBehind {
                shader.setFloatUniform("size", size.width, size.height)
                shader.setFloatUniform("time", clock.floatValue)
                shader.setFloatUniform("bg", c.background.red, c.background.green, c.background.blue, 1f)
                shader.setFloatUniform("ca", c.meshA.red, c.meshA.green, c.meshA.blue, c.meshA.alpha)
                shader.setFloatUniform("cb", c.meshB.red, c.meshB.green, c.meshB.blue, c.meshB.alpha)
                shader.setFloatUniform("cc", c.meshC.red, c.meshC.green, c.meshC.blue, c.meshC.alpha)
                drawRect(brush)
            }
        },
    )
}

private const val AURORA_AGSL = """
uniform float2 size;
uniform float time;
uniform float4 bg;
uniform float4 ca;
uniform float4 cb;
uniform float4 cc;

float ribbon(float2 uv, float y0, float phase, float speed) {
    float wave = sin(uv.x * 3.2 + time * speed + phase) * 0.09 + sin(uv.x * 7.5 - time * speed * 0.7 + phase * 1.7) * 0.03;
    float d = uv.y - (y0 + wave);
    float core = exp(-d * d * 120.0);
    float halo = exp(-abs(d) * 5.0) * 0.35;
    return core + halo;
}

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 fc) {
    float2 uv = fc / size;
    float3 col = bg.rgb;
    float a = ribbon(uv, 0.22, 0.0, 0.22) * (0.65 + 0.35 * sin(time * 0.40));
    float b = ribbon(uv, 0.48, 2.1, 0.17) * (0.65 + 0.35 * sin(time * 0.33 + 1.3));
    float c = ribbon(uv, 0.78, 4.2, 0.26) * (0.65 + 0.35 * sin(time * 0.47 + 2.6));
    col = mix(col, ca.rgb, clamp(a * 0.6, 0.0, 0.6));
    col = mix(col, cb.rgb, clamp(b * 0.6, 0.0, 0.6));
    col = mix(col, cc.rgb, clamp(c * 0.6, 0.0, 0.6));
    col += (hash(fc) - 0.5) / 255.0 * 1.5;
    return half4(col, 1.0);
}
"""

private fun DrawScope.drawParticles(c: FuriColors, t: Float) {
    val r = Random(11)
    val tint = if (c.dark) c.onSurface else c.accent
    repeat(44) { i ->
        val bx = r.nextFloat()
        val by = r.nextFloat()
        val speed = 0.012f + r.nextFloat() * 0.03f
        val rad = (1.0f + r.nextFloat() * 2.6f) * density()
        val phase = r.nextFloat() * 6.28f
        val y = ((by - t * speed) % 1f + 1f) % 1f
        val x = bx + 0.02f * sin(t * 0.3f + phase)
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 0.9f + phase))
        drawCircle(tint.copy(alpha = 0.28f * twinkle), rad, Offset(x * size.width, y * size.height))
        if (i % 6 == 0) drawCircle(tint.copy(alpha = 0.10f * twinkle), rad * 4f, Offset(x * size.width, y * size.height))
    }
}

private fun DrawScope.density() = this.drawContext.density.density

@Composable
private fun PhotoLayer(spec: ThemeSpec, shift: Modifier, scrim: Color) {
    val file = remember(spec.photoPath) { spec.photoPath?.let { File(it) } }
    Box(Modifier.fillMaxSize().then(shift)) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && spec.photoBlur > 0.01f) Modifier.blur((spec.photoBlur * 24f).dp) else Modifier,
            ),
        )
        // Scrim takes the page colour, so light themes wash the photo out and dark ones deepen it.
        Box(Modifier.fillMaxSize().background(scrim.copy(alpha = spec.photoDim.coerceIn(0.3f, 0.9f))))
    }
}

/** Phone tilt as a -1..1 offset, eased so the background glides rather than jitters. */
@Composable
fun rememberTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val tx = (-e.values[0] / 6f).coerceIn(-1f, 1f)
                val ty = (e.values[1] / 9f - 0.6f).coerceIn(-1f, 1f)
                val cur = tilt.value
                tilt.value = Offset(cur.x + (tx - cur.x) * 0.08f, cur.y + (ty - cur.y) * 0.08f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return tilt
}
