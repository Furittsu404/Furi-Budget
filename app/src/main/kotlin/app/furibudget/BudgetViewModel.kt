package app.furibudget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.core.Periods
import app.furibudget.data.AccountEntity
import app.furibudget.data.AppSettings
import app.furibudget.data.Backup
import app.furibudget.data.BackupFile
import app.furibudget.data.BudgetEntity
import app.furibudget.data.Budgeting
import app.furibudget.data.CategoryEntity
import app.furibudget.data.GoalEntity
import app.furibudget.data.Ledger
import app.furibudget.data.BillEntity
import app.furibudget.data.BillPaymentEntity
import app.furibudget.data.TemplateEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import app.furibudget.ui.theme.ThemeSpec
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetState(
    val loaded: Boolean = false,
    val currency: Currency = Currency.PHP,
    val startDay: Int = 1,
    /** Open accounts, the ones you can pick and that count toward the total. */
    val accounts: List<AccountEntity> = emptyList(),
    /** Every account including hidden ones, so old transactions still show a name. */
    val allAccounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val balances: Map<Long, Money> = emptyMap(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val billPayments: List<BillPaymentEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    val total: Money get() = accounts.fold(Money.ZERO) { a, acc -> a + (balances[acc.id] ?: Money.ZERO) }
    fun category(id: Long?) = categories.firstOrNull { it.id == id }
    fun account(id: Long?) = allAccounts.firstOrNull { it.id == id }

    /** First day of the week for weekly budgets, from the phone's locale. */
    val firstDay: java.time.DayOfWeek get() = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek

    /** The budget month containing [date], honouring the user's start day. */
    fun month(date: LocalDate = LocalDate.now()) = Periods.monthContaining(date, startDay)
}

class BudgetViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = (app as FuriBudgetApp).repository
    private val settingsRepo = (app as FuriBudgetApp).settings

    private val base = combine(repo.allAccounts, repo.categories, repo.transactions, repo.budgets, repo.goals) { a, c, t, b, g ->
        BudgetState(true, accounts = a.filter { !it.archived }, allAccounts = a, categories = c, transactions = t, balances = Ledger.balances(a, t), budgets = b, goals = g)
    }

    private val billsFlow = combine(repo.bills, repo.billPayments, repo.templates) { b, p, t -> Triple(b, p, t) }

    val state: StateFlow<BudgetState> = combine(base, billsFlow, settingsRepo.settings) { s, bills, set ->
        s.copy(bills = bills.first, billPayments = bills.second, templates = bills.third, settings = set, currency = Currency.byCode(set.currencyCode), startDay = set.startDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetState())

    init {
        viewModelScope.launch {
            repo.seedIfEmpty()
            repo.postAutoPay(LocalDate.now())
            // Force-stopping the app clears its alarms, so put the reminder back each time it opens.
            val s = settingsRepo.settings.first()
            if (s.dailyReminder) Notifier.scheduleReminder(app, s.reminderHour, s.reminderMinute)
            Notifier.scheduleBillAlerts(app, s)
        }
    }

    fun save(tx: TransactionEntity) {
        val before = state.value
        viewModelScope.launch {
            repo.save(tx)
            if (tx.kind == TxKind.EXPENSE && before.settings.budgetAlerts) checkAlerts(before, tx)
        }
    }

    /** Notifies when this expense carries a budget across 80% of its limit. */
    private fun checkAlerts(before: BudgetState, tx: TransactionEntity) {
        val today = LocalDate.now()
        val after = before.transactions.filterNot { it.id == tx.id } + tx
        for (b in before.budgets) {
            if (b.categoryId != null && b.categoryId != tx.categoryId) continue
            val range = Budgeting.rangeFor(b, today, before.startDay, before.firstDay)
            val f0 = Budgeting.status(b, before.transactions, range, before.startDay).fraction
            val st = Budgeting.status(b, after, range, before.startDay)
            if (f0 < Budgeting.CLOSE_AT && st.fraction >= Budgeting.CLOSE_AT) {
                Notifier.budgetAlert(app, before.category(b.categoryId)?.name ?: app.getString(R.string.monthly_budget), (st.fraction * 100).toInt())
            }
        }
    }

    /** Payments removed together with their expense, kept so Undo can put both back. */
    private val removedPayments = HashMap<Long, BillPaymentEntity>()

    fun delete(tx: TransactionEntity) {
        viewModelScope.launch { repo.delete(tx)?.let { removedPayments[tx.id] = it } }
    }

    fun restore(tx: TransactionEntity) {
        viewModelScope.launch { repo.restore(tx, removedPayments.remove(tx.id)) }
    }
    fun saveBudget(b: BudgetEntity) { viewModelScope.launch { repo.saveBudget(b) } }
    fun deleteBudget(b: BudgetEntity) { viewModelScope.launch { repo.deleteBudget(b) } }
    fun saveGoal(g: GoalEntity) { viewModelScope.launch { repo.saveGoal(g) } }
    fun deleteGoal(g: GoalEntity) { viewModelScope.launch { repo.deleteGoal(g) } }
    fun saveAccount(a: AccountEntity) { viewModelScope.launch { repo.saveAccount(a) } }
    fun saveCategory(c: CategoryEntity) { viewModelScope.launch { repo.saveCategory(c) } }
    fun saveBill(b: BillEntity) {
        viewModelScope.launch {
            repo.saveBill(b)
            repo.postAutoPay(LocalDate.now())
            Notifier.scheduleBillAlerts(app, settingsRepo.settings.first())
        }
    }
    fun deleteBill(b: BillEntity) { viewModelScope.launch { repo.deleteBill(b) } }

    /** Marks one due date paid and records the expense. [onDone] gets the payment, or null if it was already paid. */
    fun payBill(bill: BillEntity, due: LocalDate, amountMinor: Long, accountId: Long, paidOn: LocalDate, onDone: (BillPaymentEntity?) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.payBill(bill, due, amountMinor, accountId, paidOn)) }
    }

    /** Takes a payment back. [onDone] gets what was removed, so the caller can offer Undo. */
    fun unpay(p: BillPaymentEntity, onDone: (Pair<TransactionEntity?, BillPaymentEntity>) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.unpay(p)) }
    }

    fun redoPayment(tx: TransactionEntity?, p: BillPaymentEntity) {
        viewModelScope.launch { repo.restore(tx ?: return@launch, p) }
    }

    fun skipBill(bill: BillEntity, due: LocalDate) { viewModelScope.launch { repo.skipBill(bill, due, LocalDate.now()) } }
    fun clearPayment(p: BillPaymentEntity) { viewModelScope.launch { repo.clearPayment(p) } }
    fun saveTemplate(t: TemplateEntity) { viewModelScope.launch { repo.saveTemplate(t) } }
    fun deleteTemplate(t: TemplateEntity) { viewModelScope.launch { repo.deleteTemplate(t) } }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(change)
            val s = settingsRepo.settings.first()
            if (s.dailyReminder) Notifier.scheduleReminder(app, s.reminderHour, s.reminderMinute) else Notifier.cancelReminder(app)
            Notifier.scheduleBillAlerts(app, s)
        }
    }

    /** Runs the bill check right now (the "Check bills now" button). Returns how many notifications were sent. */
    suspend fun checkBillsNow(): Int = BillNotifier.check(app)

    suspend fun exportJson(theme: ThemeSpec?): String {
        val s = state.value
        return Backup.encode(repo.export(theme, s.currency.code, s.startDay))
    }

    fun exportCsv(): String = Backup.csv(state.value)

    /** Replaces all data with the backup in [text]. Returns the parsed file, or null when it is not a valid backup. */
    suspend fun importJson(text: String): BackupFile? {
        val file = Backup.decode(text)?.takeIf { Backup.isConsistent(it) } ?: return null
        // One transaction: if anything fails the old data is still there.
        if (runCatching { repo.replaceAll(file) }.isFailure) return null
        updateSettings { it.copy(currencyCode = file.currency ?: it.currencyCode, startDay = file.startDay ?: it.startDay) }
        return file
    }

    /** Synthetic demo data, clearly marked, so a new user can see the charts. Adds to what is there. */
    fun loadSample() {
        viewModelScope.launch {
            val s = state.value
            val acc = s.accounts.firstOrNull()?.id ?: return@launch
            val cats = s.categories.filter { it.kind == TxKind.EXPENSE }
            if (cats.isEmpty()) return@launch
            val r = java.util.Random(42)
            val today = LocalDate.now()
            for (back in 0..90) {
                val day = today.minusDays(back.toLong())
                repeat(r.nextInt(3)) {
                    val c = cats[r.nextInt(cats.size)]
                    repo.save(
                        TransactionEntity(
                            kind = TxKind.EXPENSE, amountMinor = (50 + r.nextInt(900)) * 100L, accountId = acc,
                            categoryId = c.id, note = "Sample", epochDay = day.toEpochDay(),
                        ),
                    )
                }
            }
        }
    }

    fun wipeAll() { viewModelScope.launch { repo.wipe(); repo.seedIfEmpty() } }
}
