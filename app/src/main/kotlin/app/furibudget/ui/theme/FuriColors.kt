package app.furibudget.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.furibudget.core.color.AccentDerivation
import app.furibudget.core.color.Oklch
import app.furibudget.core.color.ensureContrast
import app.furibudget.core.color.fromOklch
import app.furibudget.core.color.toOklch

/** Every colour the app draws with, already resolved for pack, brightness and accent. */
@Immutable
data class FuriColors(
    val dark: Boolean,
    val background: Color,
    val card: Color,
    val cardBorder: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** Accent tuned for text and thin strokes on [background]. */
    val accentText: Color,
    val accentGlow: Color,
    /** Second accent stop, used when the gradient accent is on (equals [accent] otherwise). */
    val accent2: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val meshA: Color,
    val meshB: Color,
    val meshC: Color,
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accent, accent2))
}

private val fallbackAccent = Accents.byId(Accents.DEFAULT_ID).argb

/** Pure function of the saved choices; no composition needed, so it is testable and cheap to animate between. */
/** Wallpaper accents from Material You (Android 12+), one per brightness. */
class DynamicAccent(val dark: Int, val light: Int)

fun resolveColors(spec: ThemeSpec, pack: ThemePack, systemDark: Boolean, dynamic: DynamicAccent?): FuriColors {
    val dark = when {
        pack.dark == null -> false
        pack.light == null -> true
        else -> when (spec.mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemDark
        }
    }
    val p = (if (dark) pack.dark else pack.light) ?: error("pack ${pack.id} has no palette")
    val accentArgb = when (val a = spec.accent) {
        is AccentChoice.Swatch -> Accents.byId(a.id).argb
        AccentChoice.Dynamic -> dynamic?.let { if (dark) it.dark else it.light } ?: fallbackAccent
        is AccentChoice.Custom -> a.argb
        is AccentChoice.Photo -> a.argb
    }
    val surface = p.background.toArgb()
    val roles = AccentDerivation.derive(accentArgb, dark, surface)
    val lch = toOklch(roles.accent)
    val second = if (spec.gradientAccent) fromOklch(Oklch(lch.l, lch.c, (lch.h + 38) % 360)) else roles.accent
    fun semantic(argb: Int) = Color(ensureContrast(argb, surface, 4.5))
    val (income, expense, warning) = if (spec.colorblindSafe) {
        Triple(semantic(0xFF3B82F6.toInt()), semantic(0xFFF97316.toInt()), semantic(0xFFEAB308.toInt()))
    } else {
        Triple(semantic(0xFF22C55E.toInt()), semantic(0xFFF43F5E.toInt()), semantic(0xFFF59E0B.toInt()))
    }
    val glow = Color(roles.glow)
    return FuriColors(
        dark = dark,
        background = p.background,
        card = p.card,
        cardBorder = p.cardBorder,
        onSurface = p.onSurface,
        onSurfaceVariant = p.onSurfaceVariant,
        accent = Color(roles.accent),
        onAccent = Color(roles.onAccent),
        accentContainer = Color(roles.container),
        onAccentContainer = Color(roles.onContainer),
        accentText = Color(roles.text),
        accentGlow = glow,
        accent2 = Color(second),
        income = income,
        expense = expense,
        warning = warning,
        meshA = p.mesh[0],
        meshB = p.mesh[1],
        meshC = glow.copy(alpha = p.accentBlob),
    )
}
