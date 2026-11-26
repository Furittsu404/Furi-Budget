package app.furibudget.core.color

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** Perceptual colour space helpers. Colours are packed ARGB ints; alpha is ignored by the maths. */
data class Oklch(val l: Double, val c: Double, val h: Double)

private fun srgbToLinear(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
private fun linearToSrgb(v: Double) = if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055

private fun channel(argb: Int, shift: Int) = ((argb ushr shift) and 0xFF) / 255.0

fun toOklch(argb: Int): Oklch {
    val r = srgbToLinear(channel(argb, 16))
    val g = srgbToLinear(channel(argb, 8))
    val b = srgbToLinear(channel(argb, 0))
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val bigL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return Oklch(bigL, hypot(a, bb), Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it })
}

/** Converts back, clamping to the sRGB gamut by reducing chroma until the colour fits. */
fun fromOklch(color: Oklch, alpha: Int = 0xFF): Int {
    var c = color.c
    repeat(24) {
        val rgb = tryRgb(color.l, c, color.h)
        if (rgb != null) return (alpha shl 24) or rgb
        c *= 0.9
    }
    val gray = tryRgb(color.l, 0.0, 0.0) ?: 0
    return (alpha shl 24) or gray
}

private fun tryRgb(lightness: Double, chroma: Double, hue: Double): Int? {
    val a = chroma * cos(Math.toRadians(hue))
    val b = chroma * sin(Math.toRadians(hue))
    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).let { it * it * it }
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).let { it * it * it }
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).let { it * it * it }
    val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val bl = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    val eps = 0.0005
    if (r < -eps || r > 1 + eps || g < -eps || g > 1 + eps || bl < -eps || bl > 1 + eps) return null
    fun pack(v: Double) = (linearToSrgb(v.coerceIn(0.0, 1.0)) * 255).let { Math.round(it).toInt() }
    return (pack(r) shl 16) or (pack(g) shl 8) or pack(bl)
}

fun relativeLuminance(argb: Int): Double {
    val r = srgbToLinear(channel(argb, 16))
    val g = srgbToLinear(channel(argb, 8))
    val b = srgbToLinear(channel(argb, 0))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** WCAG contrast ratio, 1.0 to 21.0. */
fun contrastRatio(a: Int, b: Int): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}

/**
 * Moves [fg] lighter or darker (same hue and chroma) until it reaches [min] contrast on [bg].
 * Direction follows the background: light backgrounds push darker, dark backgrounds push lighter.
 */
fun ensureContrast(fg: Int, bg: Int, min: Double): Int {
    if (contrastRatio(fg, bg) >= min) return fg
    val lch = toOklch(fg)
    val towardLight = relativeLuminance(bg) < 0.4
    var l = lch.l
    repeat(60) {
        l = if (towardLight) l + 0.015 else l - 0.015
        val next = fromOklch(Oklch(l.coerceIn(0.0, 1.0), lch.c, lch.h))
        if (contrastRatio(next, bg) >= min) return next
    }
    return if (towardLight) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
}

/** Black or white, whichever reads better on [bg]. */
fun onColor(bg: Int): Int =
    if (contrastRatio(0xFFFFFFFF.toInt(), bg) >= contrastRatio(0xFF000000.toInt(), bg)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

fun hueDistance(a: Double, b: Double): Double {
    val d = abs(a - b) % 360
    return if (d > 180) 360 - d else d
}
