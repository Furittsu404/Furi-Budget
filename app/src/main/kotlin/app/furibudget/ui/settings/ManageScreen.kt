package app.furibudget.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.AccountEntity
import app.furibudget.data.CategoryEntity
import app.furibudget.data.TxKind
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.FuriIcons
import app.furibudget.ui.components.plain
import app.furibudget.ui.theme.Accents
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard

private val categoryIcons = listOf("food", "transport", "bills", "shopping", "health", "fun", "salary", "gift", "other")

@Composable
fun ManageScreen(state: BudgetState, onSaveAccount: (AccountEntity) -> Unit, onSaveCategory: (CategoryEntity) -> Unit, onBack: () -> Unit) {
    val c = Furi.colors
    var account by remember { mutableStateOf<AccountEntity?>(null) }
    var newAccount by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var newCategory by remember { mutableStateOf<TxKind?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.accounts_categories), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        }
        Text(stringResource(R.string.accounts), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                state.accounts.forEach { a ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { account = a }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Dot(Color(a.colorArgb), a.icon)
                        Text(a.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                        Text(state.currency.format(state.balances[a.id] ?: Money.ZERO, true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = c.onSurface)
                    }
                }
            }
        }
        FilledTonalButton(onClick = { newAccount = true }) { Text(stringResource(R.string.add_account)) }

        listOf(TxKind.EXPENSE to R.string.expense_categories, TxKind.INCOME to R.string.income_categories).forEach { (kind, title) ->
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    state.categories.filter { it.kind == kind }.forEach { cat ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { category = cat }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Dot(Color(cat.colorArgb), cat.icon)
                            Text(cat.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (cat.archived) c.onSurfaceVariant else c.onSurface)
                            if (cat.archived) Text(stringResource(R.string.archived), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                        }
                    }
                }
            }
            FilledTonalButton(onClick = { newCategory = kind }) { Text(stringResource(R.string.add_category)) }
        }
    }

    if (newAccount || account != null) {
        AccountDialog(state, account, onDismiss = { newAccount = false; account = null }, onSave = { onSaveAccount(it); newAccount = false; account = null })
    }
    if (newCategory != null || category != null) {
        CategoryDialog(category, newCategory ?: TxKind.EXPENSE, state.categories.size, onDismiss = { newCategory = null; category = null }, onSave = { onSaveCategory(it); newCategory = null; category = null })
    }
}

@Composable
private fun Dot(color: Color, icon: String) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        Icon(FuriIcons.byKey(icon), null, tint = color)
    }
}

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Accents.swatches.forEach { sw ->
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(sw.argb))
                    .border(if (sw.argb == selected) 3.dp else 0.dp, Furi.colors.onSurface, CircleShape)
                    .clickable { onPick(sw.argb) },
            )
        }
    }
}

@Composable
private fun AccountDialog(state: BudgetState, existing: AccountEntity?, onDismiss: () -> Unit, onSave: (AccountEntity) -> Unit) {
    val cur = state.currency
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    val nowBalance = existing?.let { state.balances[it.id] ?: Money.ZERO } ?: Money.ZERO
    var balance by remember { mutableStateOf(if (existing == null) "" else cur.plain(nowBalance)) }
    var color by remember { mutableStateOf(existing?.colorArgb ?: Accents.swatches[(state.accounts.size * 4 + 3) % Accents.swatches.size].argb) }
    var icon by remember { mutableStateOf(existing?.icon ?: "bank") }
    val typed = cur.parse(balance.ifBlank { "0" })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_account else R.string.edit_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text(stringResource(R.string.goal_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                AmountField(balance, { balance = it }, cur, stringResource(R.string.balance_now), Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cash", "bank").forEach { k -> FilterChip(selected = icon == k, onClick = { icon = k }, label = { Text(stringResource(if (k == "cash") R.string.type_cash else R.string.type_bank)) }, leadingIcon = { Icon(FuriIcons.byKey(k), null) }) }
                }
                ColorRow(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && typed != null, onClick = {
                // The typed figure is today's balance; store what the account held before any recorded transactions.
                val start = typed!!.minor - (nowBalance.minor - (existing?.startBalance ?: 0L))
                onSave((existing ?: AccountEntity(name = "", colorArgb = 0, icon = "bank", sortOrder = state.accounts.size)).copy(name = name.trim(), colorArgb = color, icon = icon, startBalance = start))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun CategoryDialog(existing: CategoryEntity?, kind: TxKind, count: Int, onDismiss: () -> Unit, onSave: (CategoryEntity) -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember { mutableStateOf(existing?.colorArgb ?: Accents.swatches[(count * 5) % Accents.swatches.size].argb) }
    var icon by remember { mutableStateOf(existing?.icon ?: "other") }
    var archived by remember { mutableStateOf(existing?.archived ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_category else R.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(24) }, label = { Text(stringResource(R.string.goal_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryIcons.forEach { k ->
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(if (k == icon) Color(color).copy(alpha = 0.3f) else Color.Transparent)
                                .clickable { icon = k },
                            contentAlignment = Alignment.Center,
                        ) { Icon(FuriIcons.byKey(k), k, tint = Color(color)) }
                    }
                }
                ColorRow(color) { color = it }
                if (existing != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.archive_category), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(archived, { archived = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave((existing ?: CategoryEntity(name = "", colorArgb = 0, icon = "other", kind = kind, sortOrder = count)).copy(name = name.trim(), colorArgb = color, icon = icon, archived = archived))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
