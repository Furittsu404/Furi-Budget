package app.furibudget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

data class NavItem(val label: String, val icon: ImageVector)

/** Floating bar. The chosen item stretches into an accent pill with its label; the others shrink to icons. */
@Composable
fun FuriNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Furi.colors
    // The bar floats over scrolling lists; a backing keeps their icons and text from showing through the glass.
    FuriCard(modifier.clip(MaterialTheme.shapes.large).background(c.background.copy(alpha = 0.9f))) {
        Row(Modifier.height(68.dp).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { i, item ->
                val on = i == selected
                val weight by animateFloatAsState(if (on) 2.6f else 1f, Furi.motion.spring(), label = "nav-w")
                val tint by animateColorAsState(if (on) c.onAccentContainer else c.onSurfaceVariant, Furi.motion.tween(200), label = "nav-tint")
                val pill by animateColorAsState(if (on) c.accentContainer else androidx.compose.ui.graphics.Color.Transparent, Furi.motion.tween(200), label = "nav-pill")
                Row(
                    Modifier
                        .weight(weight.coerceAtLeast(0.1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(pill)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, contentDescription = if (on) null else item.label, tint = tint)
                    if (on) Text(item.label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
    }
}
