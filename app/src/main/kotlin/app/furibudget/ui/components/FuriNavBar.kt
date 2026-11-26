package app.furibudget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

data class NavItem(val label: String, val icon: ImageVector)

/** Floating bar with one accent pill that springs between destinations. */
@Composable
fun FuriNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Furi.colors
    FuriCard(modifier) {
        BoxWithConstraints(Modifier.height(68.dp).padding(6.dp)) {
            val slot = maxWidth / items.size
            val x by animateDpAsState(slot * selected, Furi.motion.spring(), label = "nav-pill")
            Box(
                Modifier
                    .offset(x = x)
                    .width(slot)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(c.accentContainer),
            )
            Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.Start) {
                items.forEachIndexed { i, item ->
                    val on = i == selected
                    val tint by animateColorAsState(if (on) c.onAccentContainer else c.onSurfaceVariant, Furi.motion.tween(200), label = "nav-tint")
                    Column(
                        Modifier
                            .width(slot)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, tint = tint)
                        Text(item.label, style = MaterialTheme.typography.labelMedium, color = tint)
                    }
                }
            }
        }
    }
}
