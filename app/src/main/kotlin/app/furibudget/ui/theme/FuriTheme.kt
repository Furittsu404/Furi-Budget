package app.furibudget.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalFuriColors = compositionLocalOf<FuriColors> { error("FuriTheme missing") }
val LocalFuriPack = compositionLocalOf { ThemePacks.glass }
val LocalFuriSpec = compositionLocalOf { ThemeSpec() }

/** Short access to the theme from any composable: `Furi.colors.accent`. */
object Furi {
    val colors: FuriColors @Composable @ReadOnlyComposable get() = LocalFuriColors.current
    val pack: ThemePack @Composable @ReadOnlyComposable get() = LocalFuriPack.current
    val spec: ThemeSpec @Composable @ReadOnlyComposable get() = LocalFuriSpec.current
    val motion: FuriMotion @Composable @ReadOnlyComposable get() = LocalFuriMotion.current

    /** Money and other figures: tabular so digits line up and tickers do not jitter. */
    val figures: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")
}

@Composable
fun FuriTheme(spec: ThemeSpec, content: @Composable () -> Unit) {
    val pack = ThemePacks.byId(spec.packId)
    val context = LocalContext.current
    val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && spec.accent == AccentChoice.Dynamic) {
        remember {
            DynamicAccent(
                dark = dynamicDarkColorScheme(context).primary.toArgb(),
                light = dynamicLightColorScheme(context).primary.toArgb(),
            )
        }
    } else null
    val target = resolveColors(spec, pack, isSystemInDarkTheme(), dynamic)
    val systemMotionOff = rememberSystemMotionOff()
    val motion = remember(pack, spec.motion, systemMotionOff) {
        FuriMotion(pack.motion, if (systemMotionOff) 0f else spec.motion)
    }
    val colors = target.animated(motion)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !colors.dark
            controller.isAppearanceLightNavigationBars = !colors.dark
        }
    }

    CompositionLocalProvider(
        LocalFuriColors provides colors,
        LocalFuriPack provides pack,
        LocalFuriSpec provides spec,
        LocalFuriMotion provides motion,
        androidx.compose.material3.LocalContentColor provides colors.onSurface,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = remember(pack) { Typography().withFamily(pack.family()) },
            shapes = remember(pack) { pack.shapes() },
            content = content,
        )
    }
}

/** Tweens every colour so a pack or accent change glides instead of cutting. */
@Composable
private fun FuriColors.animated(motion: FuriMotion): FuriColors {
    @Composable fun a(c: Color) = animateColorAsState(c, motion.tween(360), label = "color").value
    return copy(
        background = a(background), card = a(card), cardBorder = a(cardBorder),
        onSurface = a(onSurface), onSurfaceVariant = a(onSurfaceVariant),
        accent = a(accent), onAccent = a(onAccent), accentContainer = a(accentContainer),
        onAccentContainer = a(onAccentContainer), accentText = a(accentText), accentGlow = a(accentGlow),
        accent2 = a(accent2), income = a(income), expense = a(expense), warning = a(warning),
        meshA = a(meshA), meshB = a(meshB), meshC = a(meshC),
    )
}

private fun FuriColors.toMaterial(): androidx.compose.material3.ColorScheme {
    // Dialogs, sheets and text fields need opaque container colours, so mix them from the page colour.
    fun tone(a: Float) = androidx.compose.ui.graphics.lerp(background, onSurface, a)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent, onPrimary = onAccent, primaryContainer = accentContainer, onPrimaryContainer = onAccentContainer,
        secondary = accentText, onSecondary = background,
        secondaryContainer = accentContainer, onSecondaryContainer = onAccentContainer,
        tertiary = accent2, onTertiary = onAccent,
        background = background, onBackground = onSurface, surface = background, onSurface = onSurface,
        surfaceVariant = tone(0.10f), onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = background, surfaceContainerLow = tone(0.04f), surfaceContainer = tone(0.07f),
        surfaceContainerHigh = tone(0.10f), surfaceContainerHighest = tone(0.14f),
        outline = onSurfaceVariant, outlineVariant = cardBorder.copy(alpha = 1f).let { tone(0.18f) }, error = expense,
        inverseSurface = onSurface, inverseOnSurface = background,
        inversePrimary = androidx.compose.ui.graphics.lerp(accent, if (dark) Color.Black else Color.White, 0.45f),
    )
}

private fun Typography.withFamily(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

private fun ThemePack.shapes() = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(corner / 3),
    small = androidx.compose.foundation.shape.RoundedCornerShape(corner / 2),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(corner * 0.7f),
    large = androidx.compose.foundation.shape.RoundedCornerShape(corner),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(corner + 8.dp),
)
