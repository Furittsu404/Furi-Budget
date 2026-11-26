package app.furibudget.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * The app's one card. Glass packs get a frosted fill and a lit edge; outline packs get a flat fill and an accent edge.
 * Callers supply padding inside [content].
 */
@Composable
fun FuriCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Furi.colors
    val shape = MaterialTheme.shapes.large
    val fill: Modifier = when (Furi.pack.cardStyle) {
        CardStyle.GLASS -> Modifier
            .background(
                Brush.verticalGradient(listOf(c.card, c.card.copy(alpha = c.card.alpha * 0.6f))),
                shape,
            )
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(c.cardBorder, c.cardBorder.copy(alpha = c.cardBorder.alpha * 0.25f)))), shape)
        CardStyle.OUTLINE -> Modifier
            .background(c.card, shape)
            .border(BorderStroke(1.dp, c.accent.copy(alpha = 0.45f)), shape)
    }
    Column(
        modifier.clip(shape).then(fill).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}


