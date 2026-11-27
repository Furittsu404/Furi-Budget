package app.furibudget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.furibudget.R
import app.furibudget.ui.theme.Furi

/**
 * Swipe an item left to delete it. The caller owns the delete (and its Undo); the item leaves the list, so the
 * swipe state is made fresh for each [itemKey] rather than saved. A saved state would remember "already
 * swiped away" for an item that Undo brought back, and the row would come back stuck half open.
 */
@Composable
fun SwipeToDelete(
    itemKey: Any,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    val c = Furi.colors
    val deleteLabel = stringResource(R.string.delete)
    val density = LocalDensity.current
    val deleteNow by rememberUpdatedState(onDelete)
    val dismiss = remember(itemKey) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = density,
            confirmValueChange = { v -> (v == SwipeToDismissBoxValue.EndToStart).also { if (it) deleteNow() } },
            positionalThreshold = { total -> total * 0.5f },
        )
    }
    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier.semantics {
            // A swipe is not reachable with a screen reader, so offer the same action by name.
            if (enabled) customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            // progress reads 1 while the row sits still, so only draw the red layer once a swipe is under way.
            val swiping = dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val p = if (swiping) dismiss.progress.coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxSize().clip(shape).background(c.expense.copy(alpha = 0.45f * p)), contentAlignment = Alignment.CenterEnd) {
                if (swiping) Icon(Icons.Rounded.Delete, stringResource(R.string.delete), Modifier.padding(end = 20.dp).scale(0.8f + 0.4f * p), tint = c.expense)
            }
        },
    ) { content() }
}
