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
import app.furibudget.data.RecurrenceEntity
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
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val balances: Map<Long, Money> = emptyMap(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val recurrences: List<RecurrenceEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    val total: Money get() = balances.values.fold(Money.ZERO) { a, b -> a + b }
    fun category(id: Long?) = categories.firstOrNull { it.id == id }
    fun account(id: Long?) = accounts.firstOrNull { it.id == id }

    /** The budget month containing [date], honouring the user's start day. */
    fun month(date: LocalDate = LocalDate.now()) = Periods.monthContaining(date, startDay)
}

class BudgetViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = (app as FuriBudgetApp).repository
    private val settingsRepo = (app as FuriBudgetApp).settings

    private val base = combine(repo.accounts, repo.categories, repo.transactions, repo.budgets, repo.goals) { a, c, t, b, g ->
        BudgetState(true, accounts = a, categories = c, transactions = t, balances = Ledger.balances(a, t), budgets = b, goals = g)
    }

    val state: StateFlow<BudgetState> = combine(base, repo.recurrences, settingsRepo.settings) { s, r, set ->
        s.copy(recurrences = r, settings = set, currency = Currency.byCode(set.currencyCode), startDay = set.startDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetState())

    init {
        viewModelScope.launch {
            repo.seedIfEmpty()
            repo.postDueRecurrences(LocalDate.now())
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
        val month = before.month()
        val after = before.transactions.filterNot { it.id == tx.id } + tx
        for (b in before.budgets) {
            if (b.categoryId != null && b.categoryId != tx.categoryId) continue
            val f0 = Budgeting.status(b, before.transactions, month, before.startDay).fraction
            val st = Budgeting.status(b, after, month, before.startDay)
            if (f0 < Budgeting.CLOSE_AT && st.fraction >= Budgeting.CLOSE_AT) {
                Notifier.budgetAlert(app, before.category(b.categoryId)?.name ?: app.getString(R.string.monthly_budget), (st.fraction * 100).toInt())
            }
        }
    }

    fun delete(tx: TransactionEntity) { viewModelScope.launch { repo.delete(tx) } }
    fun restore(tx: TransactionEntity) { viewModelScope.launch { repo.restore(tx) } }
    fun saveBudget(b: BudgetEntity) { viewModelScope.launch { repo.saveBudget(b) } }
    fun deleteBudget(b: BudgetEntity) { viewModelScope.launch { repo.deleteBudget(b) } }
    fun saveGoal(g: GoalEntity) { viewModelScope.launch { repo.saveGoal(g) } }
    fun deleteGoal(g: GoalEntity) { viewModelScope.launch { repo.deleteGoal(g) } }
    fun saveAccount(a: AccountEntity) { viewModelScope.launch { repo.saveAccount(a) } }
    fun saveCategory(c: CategoryEntity) { viewModelScope.launch { repo.saveCategory(c) } }
    fun saveRecurrence(r: RecurrenceEntity) { viewModelScope.launch { repo.saveRecurrence(r) } }
    fun deleteRecurrence(r: RecurrenceEntity) { viewModelScope.launch { repo.deleteRecurrence(r) } }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(change)
            val s = settingsRepo.settings.first()
            if (s.dailyReminder) Notifier.scheduleReminder(app, s.reminderHour, s.reminderMinute) else Notifier.cancelReminder(app)
        }
    }

    suspend fun exportJson(theme: ThemeSpec?): String {
        val s = state.value
        return Backup.encode(repo.export(theme, s.currency.code, s.startDay))
    }

    fun exportCsv(): String = Backup.csv(state.value)

    /** Replaces all data with the backup in [text]. Returns the parsed file, or null when it is not a valid backup. */
    suspend fun importJson(text: String): BackupFile? {
        val file = Backup.decode(text) ?: return null
        repo.replaceAll(file)
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
