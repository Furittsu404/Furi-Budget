package app.furibudget.ui.settings

import app.furibudget.ui.theme.entityColor
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.components.IconPickerSheet
import app.furibudget.ui.components.plain
import app.furibudget.ui.theme.Accents
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard


@Composable
fun ManageScreen(
    state: BudgetState, onSaveAccount: (AccountEntity) -> Unit, onCheckBalance: (accountId: Long, appMinor: Long, realMinor: Long) -> Unit, onSaveCategory: (CategoryEntity) -> Unit,
    onReorderAccounts: (List<Long>) -> Unit, onReorderCategories: (List<Long>) -> Unit, onDeleteAccount: (id: Long, moveToId: Long?) -> Unit, onBack: () -> Unit,
) {
    val c = Furi.colors
    var account by remember { mutableStateOf<AccountEntity?>(null) }
    var deleting by remember { mutableStateOf<AccountEntity?>(null) }
    // Which lists are in reorder mode: "accounts", "EXPENSE" or "INCOME". Arrows only show while one is on.
    var editing by remember { mutableStateOf(setOf<String>()) }
    fun toggle(key: String) { editing = if (key in editing) editing - key else editing + key }
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
        SectionHeader(stringResource(R.string.accounts), "accounts" in editing) { toggle("accounts") }
        FuriCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                state.allAccounts.forEachIndexed { index, a ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { account = a }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Dot(entityColor(a.colorArgb), a.icon)
                        Text(a.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (a.archived) c.onSurfaceVariant else c.onSurface)
                        if (a.archived) Text(stringResource(R.string.archived), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                        Text(state.currency.format(state.balances[a.id] ?: Money.ZERO, true), style = Furi.figures.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = c.onSurface)
                        if ("accounts" in editing) MoveButtons(index, state.allAccounts.size) { to -> onReorderAccounts(state.allAccounts.map { it.id }.moved(index, to)) }
                    }
                }
            }
        }
        FilledTonalButton(onClick = { newAccount = true }) { Text(stringResource(R.string.add_account)) }

        listOf(TxKind.EXPENSE to R.string.expense_categories, TxKind.INCOME to R.string.income_categories).forEach { (kind, title) ->
            SectionHeader(stringResource(title), kind.name in editing) { toggle(kind.name) }
            val inKind = state.categories.filter { it.kind == kind }
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    inKind.forEachIndexed { index, cat ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { category = cat }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Dot(entityColor(cat.colorArgb), cat.icon)
                            Text(cat.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (cat.archived) c.onSurfaceVariant else c.onSurface)
                            if (cat.archived) Text(stringResource(R.string.archived), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                            if (kind.name in editing) MoveButtons(index, inKind.size) { to -> onReorderCategories(inKind.map { it.id }.moved(index, to)) }
                        }
                    }
                }
            }
            FilledTonalButton(onClick = { newCategory = kind }) { Text(stringResource(R.string.add_category)) }
        }
    }

    if (newAccount || account != null) {
        AccountDialog(
            state, account, onDismiss = { newAccount = false; account = null }, onSave = { onSaveAccount(it); newAccount = false; account = null }, onCheck = onCheckBalance,
            onDelete = { a -> deleting = a; newAccount = false; account = null },
        )
    }
    deleting?.let { a -> DeleteAccountDialog(state, a, onDismiss = { deleting = null }, onConfirm = { moveTo -> onDeleteAccount(a.id, moveTo); deleting = null }) }
    if (newCategory != null || category != null) {
        CategoryDialog(category, newCategory ?: TxKind.EXPENSE, state.categories.size, onDismiss = { newCategory = null; category = null }, onSave = { onSaveCategory(it); newCategory = null; category = null })
    }
}

@Composable
private fun Dot(color: Color, icon: String) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        FuriIcon(icon, tint = color)
    }
}

@Composable
fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
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
private fun AccountDialog(state: BudgetState, existing: AccountEntity?, onDismiss: () -> Unit, onSave: (AccountEntity) -> Unit, onCheck: (Long, Long, Long) -> Unit, onDelete: (AccountEntity) -> Unit) {
    var checking by remember { mutableStateOf(false) }
    val cur = state.currency
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    val nowBalance = existing?.let { state.balances[it.id] ?: Money.ZERO } ?: Money.ZERO
    var balance by remember { mutableStateOf(if (existing == null) "" else cur.plain(nowBalance)) }
    var color by remember { mutableStateOf(existing?.colorArgb ?: Accents.swatches[(state.accounts.size * 4 + 3) % Accents.swatches.size].argb) }
    var icon by remember { mutableStateOf(existing?.icon ?: "bank") }
    var hidden by remember { mutableStateOf(existing?.archived ?: false) }
    // The last open account cannot be hidden: there would be nowhere to put a transaction.
    val canHide = existing != null && (existing.archived || state.accounts.size > 1)
    val typed = cur.parse(balance.ifBlank { "0" })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_account else R.string.edit_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text(stringResource(R.string.goal_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (existing == null) AmountField(balance, { balance = it }, cur, stringResource(R.string.balance_now), Modifier.fillMaxWidth())
                else TextButton(onClick = { checking = true }) { Text(stringResource(R.string.check_balance)) }
                IconButtonRow(icon, Color(color)) { icon = it }
                ColorRow(color) { color = it }
                if (canHide) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.hide_account), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(hidden, { hidden = it })
                }
                if (existing != null) {
                    // The last open account cannot go: there would be nowhere to put a transaction.
                    val canDelete = state.accounts.any { it.id != existing.id }
                    if (canDelete) TextButton(onClick = { onDelete(existing) }) { Text(stringResource(R.string.delete_account), color = Furi.colors.expense) }
                    else Text(stringResource(R.string.last_account_hint), style = MaterialTheme.typography.bodySmall, color = Furi.colors.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && (existing != null || typed != null), onClick = {
                // A new account's typed figure is today's balance. An existing one keeps its opening balance: use Check balance to correct it.
                val start = if (existing != null) existing.startBalance else typed!!.minor
                onSave((existing ?: AccountEntity(name = "", colorArgb = 0, icon = "bank", sortOrder = state.accounts.size)).copy(name = name.trim(), colorArgb = color, icon = icon, startBalance = start, archived = hidden))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (checking && existing != null) {
        var real by remember { mutableStateOf(if (nowBalance.minor >= 0) cur.plain(nowBalance) else "") }
        val realMoney = cur.parse(real)
        val diff = (realMoney?.minor ?: nowBalance.minor) - nowBalance.minor
        AlertDialog(
            onDismissRequest = { checking = false },
            title = { Text(stringResource(R.string.check_balance)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.check_balance_hint, cur.format(nowBalance, true)), style = MaterialTheme.typography.bodyMedium)
                    AmountField(real, { real = it }, cur, stringResource(R.string.balance_really), Modifier.fillMaxWidth())
                    if (diff != 0L && realMoney != null) Text(
                        stringResource(if (diff > 0) R.string.adjust_up else R.string.adjust_down, cur.format(Money(kotlin.math.abs(diff)), true)),
                        style = MaterialTheme.typography.bodySmall, color = if (diff > 0) Furi.colors.income else Furi.colors.expense,
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = realMoney != null && diff != 0L, onClick = { onCheck(existing.id, nowBalance.minor, realMoney!!.minor); checking = false; onDismiss() }) { Text(stringResource(R.string.adjust)) }
            },
            dismissButton = { TextButton(onClick = { checking = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
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
                IconButtonRow(icon, Color(color)) { icon = it }
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

/** The current icon in a tinted bubble; tapping opens the full picker. */
@Composable
fun IconButtonRow(icon: String, tint: Color, onPick: (String) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { picking = true }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            FuriIcon(icon, tint = tint, size = 26.dp)
        }
        Text(stringResource(R.string.change_icon), style = MaterialTheme.typography.bodyLarge, color = Furi.colors.accentText)
    }
    if (picking) IconPickerSheet(icon, tint, onPick = { onPick(it); picking = false }, onDismiss = { picking = false })
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().also { val x = it.removeAt(from); it.add(to, x) }

/** A section title with an Edit button that turns reorder arrows on for the list below it. */
@Composable
private fun SectionHeader(title: String, editing: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Furi.colors.onSurface)
        TextButton(onClick = onToggle) { Text(stringResource(if (editing) R.string.done else R.string.edit)) }
    }
}

/** Up and down arrows for one row. [onMove] gets the index the row should move to. */
@Composable
private fun MoveButtons(index: Int, size: Int, onMove: (Int) -> Unit) {
    Row {
        IconButton(onClick = { onMove(index - 1) }, enabled = index > 0) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
        IconButton(onClick = { onMove(index + 1) }, enabled = index < size - 1) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
    }
}

/**
 * Asks what happens to an account's transactions before deleting it. Moving keeps every number in the app the same;
 * deleting everything removes the account's transactions and bills for good.
 */
@Composable
private fun DeleteAccountDialog(state: BudgetState, account: AccountEntity, onDismiss: () -> Unit, onConfirm: (moveToId: Long?) -> Unit) {
    val others = state.allAccounts.filter { it.id != account.id }
    var target by remember { mutableStateOf(others.firstOrNull { !it.archived }?.id ?: others.firstOrNull()?.id) }
    val txCount = state.transactions.count { it.accountId == account.id || it.toAccountId == account.id }
    val billCount = state.bills.count { it.accountId == account.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_account_title, account.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (txCount == 0 && billCount == 0) {
                    Text(stringResource(R.string.delete_account_empty), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.delete_account_body, txCount, billCount), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.move_everything_to), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        others.forEach { a -> FilterChip(selected = target == a.id, onClick = { target = a.id }, label = { Text(a.name) }, leadingIcon = { FuriIcon(a.icon, size = 18.dp, tint = entityColor(a.colorArgb)) }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (txCount == 0 && billCount == 0) null else target) }) {
                Text(stringResource(if (txCount == 0 && billCount == 0) R.string.delete else R.string.move_and_delete))
            }
        },
        dismissButton = {
            Row {
                if (txCount > 0 || billCount > 0) TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.delete_everything), color = Furi.colors.expense) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
