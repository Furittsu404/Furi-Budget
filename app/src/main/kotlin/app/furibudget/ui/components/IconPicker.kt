package app.furibudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.furibudget.R
import app.furibudget.ui.theme.Furi

private val emojiSuggestions = (
    "🍜 🍔 🍕 🍣 🍩 ☕ 🧋 🍺 🥗 🛒 🚌 🚕 🚲 ✈️ ⛽ 🏠 💡 💧 📱 🌐 🛋️ 🧹 🧺 💊 🏥 🦷 🏋️ 💇 👕 👟 👜 💍 🎁 🎮 🎬 🎵 🎤 📚 🎓 ✏️ 🧸 👶 🐶 🐱 🌱 ⛪ 🙏 💼 💰 🏦 💳 🪙 📈 🧾 🏝️ 🧳 🏨 📷 💻 🎧 🔧 🔑 🧾 ❤️ ⭐ 🎂 🥳 🍿 🛍️ 🚿 🔥 🌧️ ☀️ 🧴 🪥 🧼 🛵 🚗 🚆 🛫 🧃 🥤 🍞 🥩 🥚 🍎 🥦 🌮 🍗 🍫 🧊 🪑 🔌 🕯️ 🎒 📦 🎟️"
    ).split(" ")

/**
 * Bottom sheet for choosing an icon (or an emoji) for a category, account, bill or goal. A live preview shows the
 * choice in [tint] while you browse; tapping a tile picks it and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(selected: String, tint: Color, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Furi.colors
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<IconGroup?>(null) }
    var emojiTab by rememberSaveable { mutableStateOf(FuriIcons.isEmoji(selected)) }
    var emojiText by rememberSaveable { mutableStateOf("") }
    val shown = remember(query, group) {
        val q = query.trim().lowercase()
        IconLibrary.entries.filter { (group == null || it.group == group) && (q.isEmpty() || it.searchText.contains(q)) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().height(620.dp).padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    FuriIcon(selected, tint = tint, size = 30.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.choose_icon), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !emojiTab, onClick = { emojiTab = false }, label = { Text(stringResource(R.string.icons_tab)) })
                        FilterChip(selected = emojiTab, onClick = { emojiTab = true }, label = { Text(stringResource(R.string.emoji_tab)) })
                    }
                }
            }
            if (!emojiTab) {
                OutlinedTextField(
                    query, { query = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_icons)) },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = group == null, onClick = { group = null }, label = { Text(stringResource(R.string.filter_all)) }) }
                    items(IconGroup.entries) { g -> FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g.label) }) }
                }
                LazyVerticalGrid(GridCells.Adaptive(60.dp), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.key }) { e -> Tile(e.key, selected, tint, e.key) { onPick(e.key) } }
                }
            } else {
                OutlinedTextField(
                    emojiText, { emojiText = it.take(8) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.type_emoji)) },
                    trailingIcon = {
                        if (emojiText.isNotBlank()) Text(
                            stringResource(R.string.use), Modifier.clickable { onPick(FuriIcons.emojiKey(emojiText.trim())) }.padding(12.dp),
                            color = c.accentText, style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
                LazyVerticalGrid(GridCells.Adaptive(52.dp), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emojiSuggestions.distinct()) { em -> Tile(FuriIcons.emojiKey(em), selected, tint, em) { onPick(FuriIcons.emojiKey(em)) } }
                }
            }
        }
    }
}

@Composable
private fun Tile(key: String, selected: String, tint: Color, label: String, onClick: () -> Unit) {
    val c = Furi.colors
    val on = key == selected
    val scale by animateFloatAsState(if (on) 1.1f else 1f, Furi.motion.spring(), label = "tile")
    Box(
        Modifier
            .size(56.dp)
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .background(if (on) tint.copy(alpha = 0.28f) else c.onSurface.copy(alpha = 0.06f))
            .then(if (on) Modifier.border(2.dp, tint, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { FuriIcon(key, tint = if (on) tint else c.onSurface, size = 28.dp) }
}
