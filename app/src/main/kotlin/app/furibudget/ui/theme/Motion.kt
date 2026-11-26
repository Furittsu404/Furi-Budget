package app.furibudget.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring as composeSpring
import androidx.compose.animation.core.tween as composeTween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion for the current pack and user setting. [scale] is 0 when motion is off
 * (the app slider at 0 or the system "Remove animations" setting), 1 at full.
 */
@Immutable
class FuriMotion(private val profile: MotionProfile, val scale: Float) {
    val enabled: Boolean get() = scale > 0.01f

    /** Bouncy state change, e.g. a selection ring or a card entering. */
    fun <T> spring(): FiniteAnimationSpec<T> =
        if (!enabled) composeTween(0) else composeSpring(profile.dampingRatio, profile.stiffness * (0.6f + 0.4f * scale))

    fun <T> spring(visibilityThreshold: T): FiniteAnimationSpec<T> =
        if (!enabled) composeTween(0) else composeSpring(profile.dampingRatio, profile.stiffness * (0.6f + 0.4f * scale), visibilityThreshold)

    /** Colour and other value tweens. Short because the user is waiting on the change. */
    fun <T> tween(millis: Int = 320): FiniteAnimationSpec<T> =
        if (!enabled) composeTween(0) else composeTween((millis * (1.2f - 0.2f * scale)).toInt())

    fun <T> snap(): FiniteAnimationSpec<T> = composeTween(0)
}

val LocalFuriMotion = compositionLocalOf { FuriMotion(ThemePacks.glass.motion, 1f) }

/** True when the system animator scale is 0 ("Remove animations"). */
@Composable
fun rememberSystemMotionOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

