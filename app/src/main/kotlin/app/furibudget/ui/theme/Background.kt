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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Full-screen background behind the navigation host. Drifts slowly unless motion is off. */
@Composable
fun FuriBackground(modifier: Modifier = Modifier) {
    val c = Furi.colors
    val kind = Furi.spec.background ?: Furi.pack.defaultBackground
    val motion = Furi.motion
    val scale by rememberUpdatedState(motion.scale)
    val clock = remember { mutableFloatStateOf(0f) }
    val animating = kind == BackgroundKind.MESH && motion.enabled
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
    when {
        kind == BackgroundKind.SOLID -> Box(modifier.fillMaxSize().background(c.background))
        kind == BackgroundKind.MESH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            ShaderMesh(modifier, c, clock)
        else -> Canvas(modifier.fillMaxSize()) {
            drawBlobs(c, if (kind == BackgroundKind.MESH) clock.floatValue else 0f)
        }
    }
}

private fun Modifier.background(color: Color) = this.then(Modifier.drawWithCache { onDrawBehind { drawRect(color) } })

private fun DrawScope.drawBlobs(c: FuriColors, t: Float) {
    drawRect(c.background)
    val w = size.width
    val h = size.height
    val r = max(w, h) * 0.75f
    fun blob(color: Color, cx: Float, cy: Float) = drawRect(
        Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(cx, cy), radius = r),
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
private fun ShaderMesh(modifier: Modifier, c: FuriColors, clock: androidx.compose.runtime.MutableFloatState) {
    val shader = remember { RuntimeShader(MESH_AGSL) }
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
