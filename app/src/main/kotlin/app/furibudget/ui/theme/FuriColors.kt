package app.furibudget.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import app.furibudget.core.color.relativeLuminance
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
    val overPhoto = spec.background == BackgroundKind.PHOTO && spec.photoPath != null && spec.photoLuma != null
    val dark = when {
        pack.dark == null -> false
        pack.light == null -> true
        // Text has to read against the picture, not the system theme: a bright photo gets dark text, a dim one light text.
        overPhoto -> spec.photoLuma!! * (1f - spec.photoDim.coerceAtLeast(0.3f)) < 0.5f
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
    // Minimal colours: a plain page unless the user chose a background themselves.
    val kind = spec.background ?: if (spec.minimal) BackgroundKind.SOLID else pack.defaultBackground
    val roles0 = AccentDerivation.derive(accentArgb, dark, p.background.toArgb())
    // The accent blob follows the accent but may not get so bright (dark themes) or dark (light themes) that text on it fails.
    val glowLch = toOklch(roles0.glow)
    val blobBase = Color(fromOklch(Oklch(if (dark) minOf(glowLch.l, 0.55) else maxOf(glowLch.l, 0.85), glowLch.c, glowLch.h)))
    val blobs = listOf(p.mesh[0], p.mesh[1], blobBase.copy(alpha = p.accentBlob))
    // Worst surface text can land on: the page colour with the most contrast-hostile blob over it. Aurora ribbons reach 60% strength.
    val worst = when (kind) {
        BackgroundKind.SOLID, BackgroundKind.PHOTO -> p.background
        else -> blobs.map { b ->
            val a = if (kind == BackgroundKind.AURORA) 0.6f else b.alpha
            b.copy(alpha = a).compositeOver(p.background)
        }.let { list -> if (dark) list.maxBy { relativeLuminance(it.toArgb()) } else list.minBy { relativeLuminance(it.toArgb()) } }
    }
    val surface = worst.toArgb()
    val roles = AccentDerivation.derive(accentArgb, dark, surface)
    val lch = toOklch(roles.accent)
    val second = if (spec.gradientAccent && !spec.minimal) fromOklch(Oklch(lch.l, lch.c, (lch.h + 38) % 360)) else roles.accent
    // Strong contrast on the page colour, and still a clear 3.2 (graphics and large figures) on the worst blob, so
    // amber and red keep their hue instead of washing out to near-white on mid-tone backdrops.
    fun semantic(argb: Int) = Color(ensureContrast(ensureContrast(argb, p.background.toArgb(), 5.5), surface, 3.2))
    val (income0, expense0, warning0) = if (spec.colorblindSafe) {
        Triple(semantic(0xFF3B82F6.toInt()), semantic(0xFFF97316.toInt()), semantic(0xFFEAB308.toInt()))
    } else {
        Triple(semantic(0xFF22C55E.toInt()), semantic(0xFFF43F5E.toInt()), semantic(0xFFF59E0B.toInt()))
    }
    // Minimal colours: income takes the one accent, spending and warnings take plain ink. The + and − signs and the
    // words (Overdue, Over budget) still say what each figure is.
    val onInk = Color(ensureContrast(p.onSurface.toArgb(), surface, 7.0))
    val (income, expense, warning) = if (spec.minimal) Triple(Color(roles.text), onInk, onInk) else Triple(income0, expense0, warning0)
    val glow = Color(roles.glow)
    // For see-through packs the user picks how solid the card is. More solid means more of the page colour (dark)
    // or white (light), never a bright fill under light text, so contrast only improves as the card firms up.
    val card = spec.cardAlpha?.takeIf { p.card.alpha < 1f }?.let { a ->
        val base = if (dark) androidx.compose.ui.graphics.lerp(p.background, Color.White, 0.10f) else Color.White
        base.copy(alpha = a.coerceIn(0.05f, 1f))
    } ?: p.card
    val onSurface = Color(ensureContrast(p.onSurface.toArgb(), surface, 7.0))
    val onSurfaceVariant = Color(ensureContrast(p.onSurfaceVariant.toArgb(), surface, 4.6))
    return FuriColors(
        dark = dark,
        background = p.background,
        card = card,
        cardBorder = p.cardBorder,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
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
        meshC = blobs[2],
    )
}


/**
 * The colour to draw an account, category, bill, person or fund in. Normally its own; with minimal colours on,
 * everything uses the one accent so the screen is not a rainbow.
 */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun entityColor(argb: Int): Color = if (Furi.spec.minimal) Furi.colors.accentText else Color(argb)

/** Colour for the [index]th slice or bar of a chart. Minimal colours use fading steps of the accent instead of separate hues. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun seriesColor(argb: Int, index: Int): Color =
    if (Furi.spec.minimal) Furi.colors.accentText.copy(alpha = (1f - index * 0.14f).coerceAtLeast(0.28f)) else Color(argb)
