package app.furibudget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

data class MoreEntry(val label: String, val icon: ImageVector, val route: String)

/** The menu that opens upward from the More item: a small glass card listing the screens that do not get a tab. */
@Composable
fun MoreMenu(entries: List<MoreEntry>, current: String?, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    val c = Furi.colors
    FuriCard(modifier.width(236.dp).clip(MaterialTheme.shapes.large).background(c.background)) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.forEach { e ->
                val on = e.route == current
                Row(
                    Modifier
                        .heightIn(min = 52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (on) c.accentContainer else androidx.compose.ui.graphics.Color.Transparent)
                        .selectable(selected = on, role = Role.Button, onClick = { onPick(e.route) })
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(e.icon, null, Modifier.size(20.dp), tint = c.accentText)
                    }
                    Text(e.label, style = MaterialTheme.typography.bodyLarge, color = if (on) c.onAccentContainer else c.onSurface, maxLines = 1)
                }
            }
        }
    }
}
