package app.furibudget.ui.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.CurrencyExchange
import app.furibudget.core.Rates
import app.furibudget.core.Currency
import app.furibudget.data.CategoryEntity
import app.furibudget.data.AccountEntity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.KeypadEntry
import app.furibudget.core.Money
import app.furibudget.core.Recurrence
import app.furibudget.core.Repeat
import app.furibudget.data.BillEntity
import app.furibudget.data.BillType
import app.furibudget.data.TemplateEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TransactionSplitEntity
import app.furibudget.data.TxRole
import app.furibudget.ui.components.repeatLabel
import app.furibudget.data.TxKind
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.components.FuriIcons
import app.furibudget.ui.components.Keypad
import app.furibudget.ui.theme.Furi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Keypad text as plain values so it can go in a saved-state bundle. */
private val KeypadSaver = listSaver<KeypadEntry, Any>(
    save = { listOf(it.whole, it.frac, it.hasPoint, it.decimals) },
    restore = { KeypadEntry(it[0] as String, it[1] as String, it[2] as Boolean, it[3] as Int) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    txId: Long,
    state: BudgetState,
    onSave: (TransactionEntity, List<TransactionSplitEntity>, Set<Long>, Long?) -> Unit,
    onAddTag: (String, (Long) -> Unit) -> Unit,
    onSaveBill: (BillEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onSaveTemplate: (TemplateEntity) -> Unit,
    onDeleteTemplate: (TemplateEntity) -> Unit,
    onClose: () -> Unit,
    initialKind: TxKind = TxKind.EXPENSE,
) {
    val c = Furi.colors
    val existing = remember(txId, state.loaded) { state.transactions.firstOrNull { it.id == txId } }
    val decimals = state.currency.decimals

    // Saveable, so a half-typed amount survives rotation and the system killing the app in the background.
    val key = arrayOf<Any?>(txId, existing != null, state.accounts.isNotEmpty())
    var kind by rememberSaveable(*key) { mutableStateOf(existing?.kind ?: initialKind) }
    // A foreign-currency entry keeps what was typed in that currency plus the rate used; amountMinor is always in the main currency.
    var foreign by rememberSaveable(*key) { mutableStateOf(existing?.origCurrency) }
    var rateMicro by rememberSaveable(*key) { mutableStateOf(existing?.rateMicro) }
    val foreignCur = foreign?.let { code -> Currency.all.firstOrNull { it.code == code } }
    val entryDecimals = foreignCur?.decimals ?: decimals
    var entry by rememberSaveable(*key, stateSaver = KeypadSaver) {
        mutableStateOf(
            existing?.let { e ->
                val fc = e.origCurrency?.let { code -> Currency.all.firstOrNull { it.code == code } }
                if (fc != null && e.origAmountMinor != null) KeypadEntry.from(Money(e.origAmountMinor), fc.decimals) else KeypadEntry.from(Money(e.amountMinor), decimals)
            } ?: KeypadEntry(decimals = decimals),
        )
    }
    var accountId by rememberSaveable(*key) { mutableLongStateOf(existing?.accountId ?: state.accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by rememberSaveable(*key) { mutableStateOf(existing?.toAccountId) }
    var categoryId by rememberSaveable(*key) { mutableStateOf(existing?.categoryId) }
    var note by rememberSaveable(*key) { mutableStateOf(existing?.note.orEmpty()) }
    var dateDay by rememberSaveable(*key) { mutableLongStateOf(existing?.epochDay ?: LocalDate.now().toEpochDay()) }
    val date = LocalDate.ofEpochDay(dateDay)
    var picking by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var fundId by rememberSaveable(*key) { mutableStateOf(existing?.let { e -> state.fundMoves.firstOrNull { it.transactionId == e.id }?.fundId }) }
    var repeat by rememberSaveable { mutableStateOf(Repeat.NONE) }
    // Tags and split rows. Rows are kept as plain lists of numbers and text so they survive rotation.
    var tagIds by rememberSaveable(*key) { mutableStateOf(existing?.let { e -> state.transactionTags.filter { it.transactionId == e.id }.map { it.tagId } } ?: emptyList()) }
    val existingSplits = remember(existing, state.splits) { existing?.let { e -> state.splits.filter { it.transactionId == e.id } }.orEmpty() }
    var splitOn by rememberSaveable(*key) { mutableStateOf(existingSplits.isNotEmpty()) }
    var splitCats by rememberSaveable(*key) { mutableStateOf(existingSplits.map { it.categoryId ?: -1L }.ifEmpty { listOf(-1L, -1L) }) }
    var splitTexts by rememberSaveable(*key) { mutableStateOf(existingSplits.map { state.currency.format(Money(it.amountMinor)).replace(",", "") }.ifEmpty { listOf("", "") }) }
    val splitRows = splitCats.zip(splitTexts) { cat, text -> SplitRow(cat.takeIf { it >= 0 }, text) }

    val typed = entry.toMoney()
    val amount = if (foreignCur != null && rateMicro != null) Money(Rates.toBase(typed.minor, foreignCur, state.currency, rateMicro!!)) else typed
    val kindColor by animateColorAsState(
        when (kind) { TxKind.EXPENSE -> c.expense; TxKind.INCOME -> c.income; TxKind.TRANSFER -> c.accentText },
        Furi.motion.tween(240), label = "kind-color",
    )
    val transferTarget = toAccountId ?: state.accounts.firstOrNull { it.id != accountId }?.id
    val sliceMinor = splitRows.map { state.currency.parse(it.text)?.minor ?: 0L }
    val splitting = splitOn && kind != TxKind.TRANSFER
    val splitValid = !splitting || (sliceMinor.sum() == amount.minor && sliceMinor.all { it > 0 })
    val canSave = amount.minor > 0 && accountId != 0L && splitValid && (kind != TxKind.TRANSFER || (transferTarget != null && transferTarget != accountId))

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            Text(
                stringResource(if (existing == null) R.string.new_transaction else R.string.edit_transaction),
                Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = c.onSurface,
            )
            if (existing != null) {
                IconButton(onClick = { onDelete(existing); onClose() }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = c.expense) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (existing == null && state.templates.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.templates.forEach { t ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                kind = t.kind; foreign = null; rateMicro = null; entry = KeypadEntry.from(Money(t.amountMinor), decimals); note = t.note
                                if (state.accounts.any { it.id == t.accountId }) accountId = t.accountId
                                categoryId = t.categoryId
                            },
                            label = { Text(t.name + " · " + state.currency.format(Money(t.amountMinor))) },
                            trailingIcon = { Icon(Icons.Rounded.Close, stringResource(R.string.delete), Modifier.size(16.dp).clickable { onDeleteTemplate(t) }) },
                        )
                    }
                }
            }
            val kinds = listOf(TxKind.EXPENSE to R.string.kind_expense, TxKind.INCOME to R.string.kind_income, TxKind.TRANSFER to R.string.kind_transfer)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { i, (k, label) ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = {
                            kind = k
                            if (state.category(categoryId)?.kind != k) categoryId = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, kinds.size),
                    ) { Text(stringResource(label)) }
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                AnimatedAmount(
                    text = (foreignCur?.symbol ?: state.currency.symbol) + " " + entry.display(),
                    style = Furi.figures.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize),
                    color = kindColor,
                    rollFromZero = false,
                )
            }
            if (foreignCur != null && rateMicro != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.converted_line, state.currency.format(amount, true), foreignCur.code, state.currency.symbol + " " + Rates.display(rateMicro!!)),
                        style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant,
                    )
                }
            }
            if (kind != TxKind.TRANSFER) {
                if (splitting) {
                    Text(stringResource(R.string.split_summary, splitRows.size), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                } else {
                    CategoryTiles(state.categories.filter { it.kind == kind && !it.archived }, categoryId) { categoryId = if (categoryId == it) null else it }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AccountChip(stringResource(if (kind == TxKind.TRANSFER) R.string.from_account else R.string.account), state.accounts, accountId) { accountId = it }
                if (kind == TxKind.TRANSFER) {
                    AccountChip(stringResource(R.string.to_account), state.accounts.filter { it.id != accountId }, transferTarget) { toAccountId = it }
                }
                FilterChip(
                    selected = false,
                    onClick = { picking = true },
                    label = { Text(dateLabel(date)) },
                    leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) },
                )
                if (kind != TxKind.TRANSFER) {
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = foreignCur != null, onClick = { menu = true },
                            label = { Text(foreignCur?.code ?: stringResource(R.string.foreign_currency)) },
                            leadingIcon = { Icon(Icons.Rounded.CurrencyExchange, null, Modifier.size(18.dp)) },
                        )
                        DropdownMenu(menu, { menu = false }, Modifier.heightIn(max = 360.dp)) {
                            if (foreignCur != null) DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_currency, state.currency.code)) },
                                onClick = { foreign = null; rateMicro = null; entry = KeypadEntry(decimals = decimals); menu = false },
                            )
                            val usable = Currency.all.filter { cur -> state.rates.any { it.code == cur.code } }
                            if (usable.isEmpty()) DropdownMenuItem(text = { Text(stringResource(R.string.no_rates_for_foreign)) }, onClick = { menu = false })
                            usable.forEach { cur ->
                                DropdownMenuItem(
                                    text = { Text("${cur.symbol}  ${cur.code}") },
                                    onClick = {
                                        foreign = cur.code; rateMicro = state.rates.first { it.code == cur.code }.perBaseMicro
                                        entry = KeypadEntry(decimals = cur.decimals); menu = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (kind != TxKind.TRANSFER || existing == null) {
                    val extras = (if (splitting) 1 else 0) + (if (fundId != null && kind == TxKind.EXPENSE) 1 else 0) + tagIds.size + (if (repeat != Repeat.NONE) 1 else 0)
                    FilterChip(
                        selected = extras > 0,
                        onClick = { details = true },
                        label = { Text(if (extras > 0) stringResource(R.string.more_count, extras) else stringResource(R.string.more)) },
                        leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) },
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(80) },
                placeholder = { Text(stringResource(R.string.note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(Modifier.padding(top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Keypad(
                showPoint = entryDecimals > 0,
                onDigit = { entry = entry.digit(it) },
                onPoint = { entry = entry.point() },
                onBackspace = { entry = entry.backspace() },
                onClear = { entry = KeypadEntry(decimals = entryDecimals) },
            )
            if (canSave && existing == null) {
                var saved by rememberSaveable { mutableStateOf(false) }
                val kindLabel = stateName(kind)
                TextButton(
                    onClick = {
                        onSaveTemplate(
                            TemplateEntity(
                                name = note.trim().ifBlank { state.category(categoryId)?.name ?: kindLabel }.take(24),
                                kind = kind, amountMinor = amount.minor, accountId = accountId,
                                categoryId = if (kind == TxKind.TRANSFER) null else categoryId, note = note.trim(),
                            ),
                        )
                        saved = true
                    },
                    enabled = !saved && kind != TxKind.TRANSFER,
                ) { Text(stringResource(if (saved) R.string.template_saved else R.string.save_as_template)) }
            }
            val fb = app.furibudget.feedback.LocalFeedback.current
            Button(
                onClick = {
                    fb.saved()
                    onSave(
                        TransactionEntity(
                            id = existing?.id ?: 0,
                            kind = kind,
                            amountMinor = amount.minor,
                            accountId = accountId,
                            toAccountId = if (kind == TxKind.TRANSFER) transferTarget else null,
                            categoryId = if (kind == TxKind.TRANSFER || splitting) null else categoryId,
                            note = note.trim(),
                            epochDay = date.toEpochDay(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            role = existing?.role ?: TxRole.NORMAL, loanId = existing?.loanId,
                            origAmountMinor = if (foreignCur != null && kind != TxKind.TRANSFER) typed.minor else null,
                            origCurrency = if (foreignCur != null && kind != TxKind.TRANSFER) foreignCur.code else null,
                            rateMicro = if (foreignCur != null && kind != TxKind.TRANSFER) rateMicro else null,
                        ),
                        if (splitting) splitRows.mapIndexed { i, r -> TransactionSplitEntity(transactionId = existing?.id ?: 0, categoryId = r.categoryId, amountMinor = sliceMinor[i]) } else emptyList(),
                        if (kind == TxKind.TRANSFER) emptySet() else tagIds.toSet(),
                        if (kind == TxKind.EXPENSE) fundId else null,
                    )
                    if (repeat != Repeat.NONE && existing == null) {
                        // The transaction above is the first one; the bill takes over from the next due date, paying itself.
                        val cat = state.category(categoryId)
                        onSaveBill(
                            BillEntity(
                                name = note.trim().ifBlank { cat?.name ?: "Recurring" }, kind = kind, type = BillType.RECURRING,
                                amountMinor = amount.minor, accountId = accountId,
                                toAccountId = if (kind == TxKind.TRANSFER) transferTarget else null,
                                categoryId = if (kind == TxKind.TRANSFER) null else categoryId,
                                repeat = repeat, anchorDay = date.dayOfMonth,
                                firstDueEpochDay = Recurrence.next(date, repeat, date.dayOfMonth).toEpochDay(),
                                autoPay = true, remindDaysBefore = -1,
                                colorArgb = cat?.colorArgb ?: 0xFF8B7CFF.toInt(),
                            ),
                        )
                    }
                    onClose()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (details) {
        ModalBottomSheet(onDismissRequest = { details = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.more_options), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                if (kind != TxKind.TRANSFER) {
                    SplitEditor(
                        on = splitOn, onToggle = { splitOn = it },
                        rows = splitRows, onRows = { r -> splitCats = r.map { it.categoryId ?: -1L }; splitTexts = r.map { it.text } },
                        total = amount, currency = state.currency, categories = state.categories.filter { it.kind == kind && !it.archived },
                    )
                    TagsRow(
                        state.tags, tagIds.toSet(),
                        onToggle = { id -> tagIds = if (id in tagIds) tagIds - id else tagIds + id },
                        onAdd = { n -> onAddTag(n) { id -> tagIds = tagIds + id } },
                    )
                }
                if (kind == TxKind.EXPENSE && state.funds.isNotEmpty()) {
                    ChipRow(stringResource(R.string.pay_from_fund)) {
                        state.funds.forEach { f ->
                            FilterChip(selected = fundId == f.id, onClick = { fundId = if (fundId == f.id) null else f.id }, label = { Text(f.name) }, leadingIcon = { FuriIcon(f.icon, size = 18.dp, tint = Color(f.colorArgb)) })
                        }
                    }
                }
                if (existing == null) {
                    ChipRow(stringResource(R.string.repeat)) {
                        Repeat.entries.forEach { r -> FilterChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(repeatLabel(r)) }) }
                    }
                }
                Button(onClick = { details = false }, Modifier.fillMaxWidth().padding(bottom = 16.dp)) { Text(stringResource(R.string.done)) }
            }
        }
    }

    if (picking) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun stateName(kind: TxKind) = stringResource(when (kind) { TxKind.EXPENSE -> R.string.kind_expense; TxKind.INCOME -> R.string.kind_income; TxKind.TRANSFER -> R.string.kind_transfer })

/** Big tappable category tiles: an icon disc over the name. The chosen one gets a ring in its own colour. */
@Composable
private fun CategoryTiles(categories: List<CategoryEntity>, selected: Long?, onPick: (Long) -> Unit) {
    val c = Furi.colors
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.forEach { cat ->
            val on = cat.id == selected
            val col = Color(cat.colorArgb)
            val scale by animateFloatAsState(if (on) 1.06f else 1f, Furi.motion.spring(), label = "cat-tile")
            Column(
                Modifier
                    .width(76.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onPick(cat.id) }
                    .semantics { role = Role.RadioButton; this.selected = on }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(col.copy(alpha = if (on) 0.34f else 0.16f))
                        .then(if (on) Modifier.border(2.dp, col, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) { FuriIcon(cat.icon, tint = col, size = 26.dp) }
                Text(cat.name, style = MaterialTheme.typography.labelMedium, color = if (on) c.onSurface else c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** One chip showing the chosen account; tapping opens a menu of the others. */
@Composable
private fun AccountChip(label: String, accounts: List<AccountEntity>, selectedId: Long?, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val acc = accounts.firstOrNull { it.id == selectedId }
    Box {
        FilterChip(
            selected = false, onClick = { open = true },
            label = { Text(acc?.name ?: label) },
            leadingIcon = { if (acc != null) FuriIcon(acc.icon, size = 18.dp, tint = Color(acc.colorArgb)) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp)) },
        )
        DropdownMenu(open, { open = false }) {
            accounts.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.name) }, onClick = { onPick(a.id); open = false },
                    leadingIcon = { FuriIcon(a.icon, size = 20.dp, tint = Color(a.colorArgb)) },
                )
            }
        }
    }
}
