package app.furibudget.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Icon keys are stored in the database. A key is either a library icon name or "e:" followed by an emoji. */
object FuriIcons {
    const val EMOJI_PREFIX = "e:"

    fun isEmoji(key: String) = key.startsWith(EMOJI_PREFIX)
    fun emojiOf(key: String) = key.removePrefix(EMOJI_PREFIX)
    fun emojiKey(emoji: String) = EMOJI_PREFIX + emoji

    /** The vector for a library key, or the generic category icon when the key is unknown. */
    fun byKey(key: String): ImageVector = IconLibrary.find(key)?.vector ?: Icons.Rounded.Category
}

/** Draws a stored icon key: a library icon tinted with [tint], or an emoji at the same size. */
@Composable
fun FuriIcon(key: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, size: Dp = 24.dp) {
    if (FuriIcons.isEmoji(key)) {
        Text(FuriIcons.emojiOf(key), modifier.size(size), fontSize = (size.value * 0.8f).sp)
    } else {
        Icon(FuriIcons.byKey(key), null, modifier.size(size), tint = tint)
    }
}
