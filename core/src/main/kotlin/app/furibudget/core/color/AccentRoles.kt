package app.furibudget.core.color

/** Colour roles derived from one accent so any accent, built-in or custom, stays readable. */
data class AccentRoles(
    val accent: Int,
    val onAccent: Int,
    val container: Int,
    val onContainer: Int,
    /** Accent tuned for text and thin strokes on the page surface. */
    val text: Int,
    val glow: Int,
)

object AccentDerivation {
    /** Floor for accent text on the surface (WCAG AA body text). */
    const val TEXT_CONTRAST = 4.5

    fun derive(accent: Int, dark: Boolean, surface: Int): AccentRoles {
        val base = toOklch(accent)
        // Fills keep the accent's own lightness band; very pale or very dark picks get pulled into a usable range.
        val fillL = if (dark) base.l.coerceIn(0.62, 0.86) else base.l.coerceIn(0.40, 0.66)
        val fill = fromOklch(Oklch(fillL, base.c, base.h))
        val containerL = if (dark) 0.30 else 0.92
        val container = fromOklch(Oklch(containerL, (base.c * 0.55).coerceAtMost(0.12), base.h))
        val onContainer = ensureContrast(fromOklch(Oklch(if (dark) 0.88 else 0.30, base.c * 0.6, base.h)), container, 7.0)
        val text = ensureContrast(fill, surface, TEXT_CONTRAST)
        val glow = fromOklch(Oklch((base.l + 0.05).coerceAtMost(0.95), base.c, base.h))
        return AccentRoles(fill, onColor(fill), container, onContainer, text, glow)
    }

    /** Evenly spaced hues around the accent for chart series, each held to [minContrast] on [surface]. */
    fun chartSeries(accent: Int, count: Int, dark: Boolean, surface: Int, minContrast: Double = 3.0): List<Int> {
        val base = toOklch(accent)
        val l = if (dark) 0.74 else 0.55
        val c = base.c.coerceIn(0.09, 0.16)
        return List(count) { i ->
            val h = (base.h + i * 360.0 / count) % 360
            ensureContrast(fromOklch(Oklch(l, c, h)), surface, minContrast)
        }
    }
}
