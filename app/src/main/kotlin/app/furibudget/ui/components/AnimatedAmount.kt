package app.furibudget.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import app.furibudget.ui.theme.Furi

/**
 * Money that rolls digit by digit when it changes. On first show it rolls up from zeros.
 * Pass the full formatted text; non-digits stay still.
 */
@Composable
fun AnimatedAmount(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, rollFromZero: Boolean = true) {
    var shown by remember { mutableStateOf(if (rollFromZero) text.map { if (it.isDigit()) '0' else it }.joinToString("") else text) }
    LaunchedEffect(text) { shown = text }
    val motion = Furi.motion
    Row(modifier.semantics { contentDescription = text }) {
        // Align from the right so a change in length keeps the cents in place.
        val padded = shown
        padded.forEachIndexed { i, ch ->
            if (!ch.isDigit()) {
                Text(ch.toString(), style = style, color = color)
            } else {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        if (!motion.enabled) ContentTransform(EnterTransition.None, ExitTransition.None)
                        else {
                            val up = targetState > initialState
                            val dir = if (up) 1 else -1
                            (slideInVertically(motion.spring()) { dir * it } + fadeIn(motion.tween(160))) togetherWith
                                (slideOutVertically(motion.spring()) { -dir * it } + fadeOut(motion.tween(120)))
                        }
                    },
                    modifier = Modifier.clipToBounds(),
                    label = "digit$i",
                ) { c -> Text(c.toString(), style = style, color = color) }
            }
        }
    }
}
