package app.furibudget.ui.people

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.furibudget.BudgetState
import app.furibudget.R
import app.furibudget.core.Money
import app.furibudget.data.Budgeting
import app.furibudget.data.LoanDirection
import app.furibudget.data.LoanStatus
import app.furibudget.data.People
import app.furibudget.data.PersonEntity
import app.furibudget.ui.components.AmountField
import app.furibudget.ui.components.AnimatedAmount
import app.furibudget.ui.components.BudgetRing
import app.furibudget.ui.components.ConfettiBurst
import app.furibudget.ui.components.FuriIcon
import app.furibudget.ui.theme.Furi
import app.furibudget.ui.theme.FuriCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** One person: what is open between you, each loan with its progress, and the buttons to lend, borrow and repay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    personId: Long,
    state: BudgetState,
    onLend: (Long, LoanDirection, Long, Long, Long?, String) -> Unit,
    onRepay: (loanId: Long, amountMinor: Long, accountId: Long) -> Unit,
    onSavePerson: (PersonEntity) -> Unit,
    onDeletePerson: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val c = Furi.colors
    val cur = state.currency
    val balance = remember(state.people, state.loans, state.transactions, personId) {
        People.balances(state.people.filter { it.id == personId }, state.loans, state.transactions).firstOrNull()
    }
    if (balance == null) { LaunchedEffect(Unit) { onBack() }; return }
    val person = balance.person
    val net = balance.netMinor
    var lending by rememberSaveable { mutableStateOf<LoanDirection?>(null) }
    var repaying by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val settledNow = balance.loans.count { it.settled }
    var confetti by rememberSaveable { mutableIntStateOf(0) }
    var seen by rememberSaveable { mutableIntStateOf(settledNow) }
    val fb = app.furibudget.feedback.LocalFeedback.current
    LaunchedEffect(settledNow) { if (settledNow > seen) { confetti++; fb.goal() }; seen = settledNow }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                Text(person.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = c.onSurface, maxLines = 1)
                IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit_person)) }
                IconButton(onClick = { onDeletePerson(person.id); onBack() }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = c.expense) }
            }
            FuriCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(person, 64.dp)
                    Text(
                        when { net > 0 -> stringResource(R.string.owes_you); net < 0 -> stringResource(R.string.you_owe_them); else -> stringResource(R.string.all_settled) },
                        style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant,
                    )
                    AnimatedAmount(
                        cur.symbol + " " + cur.format(Money(kotlin.math.abs(net))),
                        Furi.figures.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                        when { net > 0 -> c.income; net < 0 -> c.expense; else -> c.onSurface },
                    )
                    BalanceBar(balance.owedToYouMinor, balance.youOweMinor)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { lending = LoanDirection.LENT }, Modifier.weight(1f)) { Text(stringResource(R.string.lend)) }
                FilledTonalButton(onClick = { lending = LoanDirection.OWED }, Modifier.weight(1f)) { Text(stringResource(R.string.borrow)) }
            }
            if (balance.loans.isEmpty()) {
                Text(stringResource(R.string.no_loans_yet), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            balance.loans.sortedWith(compareBy({ it.settled }, { -it.loan.openedEpochDay })).forEach { st ->
                LoanCard(st, state) { repaying = st.loan.id }
            }
        }
        ConfettiBurst(confetti, listOf(c.income, c.accent, c.warning), Modifier.fillMaxSize())
    }

    lending?.let { dir ->
        LoanSheet(dir, state, onDismiss = { lending = null }) { amt, acc, due, note -> onLend(person.id, dir, amt, acc, due, note); lending = null }
    }
    repaying?.let { id ->
        val st = balance.loans.firstOrNull { it.loan.id == id }
        if (st == null) repaying = null else RepaySheet(st, state, onDismiss = { repaying = null }) { amt, acc -> onRepay(id, amt, acc); repaying = null }
    }
    if (editing) PersonDialog(person, onDismiss = { editing = false }, onSave = { onSavePerson(it); editing = false })
}

@Composable
private fun LoanCard(st: LoanStatus, state: BudgetState, onRepay: () -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    val lent = st.loan.direction == LoanDirection.LENT
    val today = LocalDate.now()
    val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    FuriCard(Modifier.fillMaxWidth().alpha(if (st.settled) 0.7f else 1f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BudgetRing(st.fraction, if (st.overdue(today)) Budgeting.Level.OVER else Budgeting.Level.OK, Modifier.size(60.dp), stroke = 7.dp) {
                if (st.settled) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(24.dp), tint = c.income)
                else Text("${(st.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = c.onSurface)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(if (lent) R.string.you_lent else R.string.you_borrowed, cur.format(Money(st.loan.principalMinor), true)),
                    style = MaterialTheme.typography.bodyLarge, color = c.onSurface,
                )
                Text(
                    if (st.settled) stringResource(R.string.loan_settled) else stringResource(R.string.still_open, cur.format(Money(st.outstandingMinor), true)),
                    style = MaterialTheme.typography.bodySmall, color = if (st.overdue(today)) c.expense else c.onSurfaceVariant,
                )
                val meta = buildList {
                    add(LocalDate.ofEpochDay(st.loan.openedEpochDay).format(dateFmt))
                    st.loan.dueEpochDay?.let { add(stringResource(R.string.due_on, LocalDate.ofEpochDay(it).format(dateFmt))) }
                    if (st.loan.note.isNotBlank()) add(st.loan.note)
                }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, maxLines = 2)
            }
            if (!st.settled) FilledTonalButton(onClick = onRepay) { Text(stringResource(if (lent) R.string.record_payback else R.string.record_payment)) }
        }
    }
}

@Composable
private fun AccountPicker(state: BudgetState, selected: Long, onPick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.account), style = MaterialTheme.typography.labelLarge, color = Furi.colors.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.accounts.forEach { a ->
                FilterChip(selected = selected == a.id, onClick = { onPick(a.id) }, label = { Text(a.name) }, leadingIcon = { FuriIcon(a.icon, size = 18.dp, tint = Color(a.colorArgb)) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanSheet(dir: LoanDirection, state: BudgetState, onDismiss: () -> Unit, onSave: (Long, Long, Long?, String) -> Unit) {
    val c = Furi.colors
    var amount by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf(state.accounts.firstOrNull()?.id ?: 0L) }
    var dueDays by rememberSaveable { mutableIntStateOf(0) }
    var note by rememberSaveable { mutableStateOf("") }
    val money = state.currency.parse(amount)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (dir == LoanDirection.LENT) R.string.lend else R.string.borrow), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            Text(stringResource(if (dir == LoanDirection.LENT) R.string.lend_hint else R.string.borrow_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            AmountField(amount, { amount = it }, state.currency, stringResource(R.string.amount), Modifier.fillMaxWidth())
            AccountPicker(state, account) { account = it }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.due), style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to R.string.no_due_date, 7 to R.string.in_a_week, 14 to R.string.in_two_weeks, 30 to R.string.in_a_month).forEach { (d, label) ->
                        FilterChip(selected = dueDays == d, onClick = { dueDays = d }, label = { Text(stringResource(label)) })
                    }
                }
            }
            OutlinedTextField(note, { note = it.take(60) }, singleLine = true, label = { Text(stringResource(R.string.note)) }, modifier = Modifier.fillMaxWidth())
            Button(
                enabled = (money?.minor ?: 0) > 0 && account != 0L,
                onClick = { onSave(money!!.minor, account, if (dueDays == 0) null else LocalDate.now().plusDays(dueDays.toLong()).toEpochDay(), note.trim()) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepaySheet(st: LoanStatus, state: BudgetState, onDismiss: () -> Unit, onSave: (Long, Long) -> Unit) {
    val c = Furi.colors
    val cur = state.currency
    var amount by rememberSaveable { mutableStateOf(cur.format(Money(st.outstandingMinor)).replace(",", "")) }
    var account by rememberSaveable { mutableStateOf(state.accounts.firstOrNull()?.id ?: 0L) }
    val asked = cur.parse(amount)?.minor ?: 0L
    val capped = People.cap(asked, st)
    val full = capped == st.outstandingMinor
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (st.loan.direction == LoanDirection.LENT) R.string.record_payback else R.string.record_payment), style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            Text(stringResource(R.string.still_open, cur.format(Money(st.outstandingMinor), true)), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            AmountField(amount, { amount = it }, cur, stringResource(R.string.amount), Modifier.fillMaxWidth())
            if (asked > st.outstandingMinor) Text(stringResource(R.string.more_than_open), style = MaterialTheme.typography.bodySmall, color = c.expense)
            AccountPicker(state, account) { account = it }
            Button(
                enabled = capped > 0 && account != 0L && asked <= st.outstandingMinor,
                onClick = { onSave(capped, account) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) { Text(stringResource(if (full) R.string.settle else R.string.record)) }
        }
    }
}
