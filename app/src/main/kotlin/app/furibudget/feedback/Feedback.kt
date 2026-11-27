package app.furibudget.feedback

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import app.furibudget.data.AppSettings

/** The events that can make a sound. */
object FeedbackEvent {
    const val KEYPAD = "keypad"
    const val SAVED = "saved"
    const val PAID = "paid"
    const val DELETED = "deleted"
    const val GOAL = "goal"
}

/**
 * One place for every tap of haptics and every sound, driven by the user's settings. Haptics go through the view,
 * so the phone's own touch-feedback setting still decides whether anything is felt.
 */
class Feedback(private val view: View?, private val settings: AppSettings, private val player: SoundPlayer?) {
    private fun vibrate(kind: Int) {
        if (!settings.hapticsOn || view == null) return
        val s = settings.hapticStrength.coerceIn(0, 2)
        val c = when (kind) {
            TICK -> intArrayOf(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.CONTEXT_CLICK)[s]
            ACT -> intArrayOf(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackConstants.LONG_PRESS)[s]
            REJECT -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            else -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(c)
    }

    private fun sound(event: String) {
        if (!settings.soundOn) return
        player?.play(settings.soundStyles[event] ?: "soft", event, settings.soundVolume / 100f)
    }

    /** A keypad press. */
    fun keypad() { vibrate(TICK); sound(FeedbackEvent.KEYPAD) }
    /** A light tick for scrubbing and choosing; never a sound. */
    fun selection() = vibrate(TICK)
    fun saved() { vibrate(ACT); sound(FeedbackEvent.SAVED) }
    fun paid() { vibrate(CONFIRM); sound(FeedbackEvent.PAID) }
    fun deleted() { vibrate(REJECT); sound(FeedbackEvent.DELETED) }
    fun goal() { vibrate(CONFIRM); sound(FeedbackEvent.GOAL) }
    /** The settings screen's "try it": plays regardless of the master switches' current values being on. */
    fun previewHaptic() = vibrate(ACT)

    companion object {
        private const val TICK = 0
        private const val ACT = 1
        private const val REJECT = 2
        private const val CONFIRM = 3
        val None = Feedback(null, AppSettings(hapticsOn = false), null)
    }
}

val LocalFeedback = staticCompositionLocalOf { Feedback.None }
