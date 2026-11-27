package app.furibudget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.core.Periods
import app.furibudget.data.AccountEntity
import app.furibudget.data.FundEntity
import app.furibudget.data.FundMoveEntity
import app.furibudget.data.LoanDirection
import app.furibudget.data.LoanEntity
import app.furibudget.data.PersonEntity
import app.furibudget.data.RateEntity
import app.furibudget.data.RateFetcher
import app.furibudget.data.TagEntity
import app.furibudget.data.TransactionSplitEntity
import app.furibudget.data.TransactionTagEntity
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    val tags: List<TagEntity> = emptyList(),
    val rates: List<RateEntity> = emptyList(),
    val funds: List<FundEntity> = emptyList(),
    val fundMoves: List<FundMoveEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val transactionTags: List<TransactionTagEntity> = emptyList(),
    val splits: List<TransactionSplitEntity> = emptyList(),
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

private suspend fun Application.widgetUpdate() = app.furibudget.widget.WidgetUpdater.update(this)

/** How long deleted items stay in Recently deleted. */
const val TRASH_DAYS = 30L

/** Everything in Recently deleted, newest first. */
data class Trash(val transactions: List<TransactionEntity> = emptyList(), val bills: List<BillEntity> = emptyList(), val goals: List<GoalEntity> = emptyList(), val people: List<PersonEntity> = emptyList(), val funds: List<FundEntity> = emptyList()) {
    val isEmpty: Boolean get() = transactions.isEmpty() && bills.isEmpty() && goals.isEmpty() && people.isEmpty() && funds.isEmpty()
}

class BudgetViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = (app as FuriBudgetApp).repository
    private val settingsRepo = (app as FuriBudgetApp).settings

    private val base = combine(repo.allAccounts, repo.categories, repo.transactions, repo.budgets, repo.goals) { a, c, t, b, g ->
        BudgetState(true, accounts = a.filter { !it.archived }, allAccounts = a, categories = c, transactions = t, balances = Ledger.balances(a, t), budgets = b, goals = g)
    }

    private val billsFlow = combine(repo.bills, repo.billPayments, repo.templates) { b, p, t -> Triple(b, p, t) }

    private val tagFlow = combine(repo.tags, repo.transactionTags, repo.splits) { t, l, s -> Triple(t, l, s) }
    private val peopleFlow = combine(repo.people, repo.loans, repo.funds, repo.fundMoves, repo.rates) { p, l, f, m, r -> listOf(p, l, f, m, r) }

    val state: StateFlow<BudgetState> = combine(base, billsFlow, settingsRepo.settings, tagFlow, peopleFlow) { s, bills, set, tg, pl ->
        s.copy(bills = bills.first, billPayments = bills.second, templates = bills.third, rates = pl[4] as List<RateEntity>, people = pl[0] as List<PersonEntity>, loans = pl[1] as List<LoanEntity>, funds = pl[2] as List<FundEntity>, fundMoves = pl[3] as List<FundMoveEntity>, tags = tg.first, transactionTags = tg.second, splits = tg.third, settings = set, currency = Currency.byCode(set.currencyCode), startDay = set.startDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetState())

    val trash: StateFlow<Trash> = combine(repo.deletedTransactions, repo.deletedBills, repo.deletedGoals, repo.deletedPeople, repo.deletedFunds) { t, b, g, p, f -> Trash(t, b, g, p, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Trash())

    private val _fetching = kotlinx.coroutines.flow.MutableStateFlow(false)
    /** True while an exchange-rate request is running. */
    val fetchingRates: StateFlow<Boolean> = _fetching

    /** Fetches rates when the switch is on and the cache is older than a day (or [force]). Silent on failure. */
    fun refreshRates(force: Boolean = false) {
        val s = state.value.settings
        if (!s.ratesOnline || _fetching.value) return
        val stale = System.currentTimeMillis() - s.ratesFetchedAt > 24 * 3_600_000L
        if (!force && !stale) return
        viewModelScope.launch {
            _fetching.value = true
            try {
                val got = RateFetcher.fetch(s.currencyCode)
                if (got != null) {
                    val now = System.currentTimeMillis()
                    repo.storeOnlineRates(got, now)
                    settingsRepo.update { it.copy(ratesFetchedAt = now) }
                }
            } finally { _fetching.value = false }
        }
    }
    /** Changing the main currency makes every cached rate meaningless, so drop them and fetch fresh ones. */
    fun setCurrency(code: String) {
        viewModelScope.launch {
            if (state.value.settings.currencyCode != code) {
                repo.clearAllRates()
                settingsRepo.update { it.copy(currencyCode = code, ratesFetchedAt = 0L) }
                refreshRates(force = true)
            }
        }
    }
    /** Called right after the switch is turned on: the settings write has to land before the fetch reads it. */
    fun refreshRatesSoon() { viewModelScope.launch { state.first { it.settings.ratesOnline }; refreshRates(force = true) } }
    private val player get() = (app as FuriBudgetApp).sounds
    fun hasCustomSound(event: String) = player.hasCustom(event)
    fun clearSound(event: String) = player.clearCustom(event)
    /** Plays a style at the saved volume, even when the master switch is off, so you can hear it before choosing. */
    fun previewSound(style: String, event: String) { player.play(style, event, state.value.settings.soundVolume.coerceAtLeast(20) / 100f) }
    fun importSound(uri: android.net.Uri, event: String, onDone: (app.furibudget.feedback.SoundPlayer.ImportError?) -> Unit) {
        viewModelScope.launch { onDone(player.importCustom(uri, event)) }
    }
    fun setManualRate(code: String, rateMicro: Long) { viewModelScope.launch { repo.setManualRate(code, rateMicro, System.currentTimeMillis()) } }
    fun clearRate(code: String) { viewModelScope.launch { repo.clearRate(code) } }

    fun saveFund(f: FundEntity) { viewModelScope.launch { repo.saveFund(f) } }
    fun deleteFund(id: Long) { viewModelScope.launch { repo.softDeleteFund(id, System.currentTimeMillis()) } }
    fun restoreFund(id: Long) { viewModelScope.launch { repo.restoreFund(id) } }
    fun deleteFundForever(id: Long) { viewModelScope.launch { repo.deleteFundForever(id) } }
    fun moveFund(fundId: Long, amountMinor: Long) { viewModelScope.launch { repo.moveFund(fundId, amountMinor, LocalDate.now().toEpochDay()) } }
    fun checkBalance(accountId: Long, appMinor: Long, realMinor: Long, note: String) {
        viewModelScope.launch { repo.adjustBalance(accountId, appMinor, realMinor, LocalDate.now().toEpochDay(), note) }
    }
    fun savePerson(p: PersonEntity, onDone: (Long) -> Unit = {}) { viewModelScope.launch { onDone(repo.savePerson(p)) } }
    fun deletePerson(id: Long) { viewModelScope.launch { repo.softDeletePerson(id, System.currentTimeMillis()) } }
    fun restorePerson(id: Long) { viewModelScope.launch { repo.restorePerson(id) } }
    fun deletePersonForever(id: Long) { viewModelScope.launch { repo.deletePersonForever(id) } }
    fun lend(personId: Long, direction: LoanDirection, amountMinor: Long, accountId: Long, epochDay: Long, dueEpochDay: Long?, note: String) {
        viewModelScope.launch { repo.lend(personId, direction, amountMinor, accountId, epochDay, dueEpochDay, note) }
    }
    fun repay(loanId: Long, amountMinor: Long, accountId: Long, epochDay: Long) { viewModelScope.launch { repo.repay(loanId, amountMinor, accountId, epochDay) } }
    fun restoreBill(id: Long) { viewModelScope.launch { repo.restoreBill(id) } }
    fun restoreGoal(id: Long) { viewModelScope.launch { repo.restoreGoal(id) } }
    fun deleteForever(tx: TransactionEntity) { viewModelScope.launch { repo.deleteTxForever(tx) } }
    fun deleteForever(b: BillEntity) { viewModelScope.launch { repo.deleteBillForever(b) } }
    fun deleteForever(g: GoalEntity) { viewModelScope.launch { repo.deleteGoalForever(g) } }

    init {
        // The three most used templates become long-press shortcuts on the launcher icon.
        viewModelScope.launch { repo.templates.collect { Shortcuts.publishTemplates(app, it) } }
        // Keep any home-screen widget current: redraw when money, bills or payments change.
        viewModelScope.launch {
            state.map { Triple(it.total, it.transactions.size, it.billPayments.size + it.bills.size) }.distinctUntilChanged().collect {
                if (state.value.loaded) app.widgetUpdate()
            }
        }
        viewModelScope.launch {
            repo.seedIfEmpty()
            repo.purgeDeleted(System.currentTimeMillis(), TRASH_DAYS * 86_400_000L)
            repo.postAutoPay(LocalDate.now())
            repo.postFundMonthly(LocalDate.now())
            refreshRates()
            // Force-stopping the app clears its alarms, so put the reminder back each time it opens.
            val s = settingsRepo.settings.first()
            if (s.dailyReminder) Notifier.scheduleReminder(app, s.reminderHour, s.reminderMinute)
            Notifier.scheduleBillAlerts(app, s)
        }
    }

    fun save(tx: TransactionEntity, splits: List<TransactionSplitEntity> = emptyList(), tagIds: Set<Long> = emptySet(), fundId: Long? = null) {
        val before = state.value
        viewModelScope.launch {
            val id = repo.saveFull(tx, splits, tagIds, fundId)
            if (tx.kind == TxKind.EXPENSE && before.settings.budgetAlerts) checkAlerts(before, tx.copy(id = id), splits)
        }
    }

    /** Creates a tag and hands back its id, so the editor can select it straight away. */
    fun addTag(name: String, colorArgb: Int, onDone: (Long) -> Unit) {
        viewModelScope.launch { onDone(repo.saveTag(TagEntity(name = name.trim().take(24), colorArgb = colorArgb))) }
    }
    fun deleteTag(t: TagEntity) { viewModelScope.launch { repo.deleteTag(t) } }

    /** Notifies when this expense carries a budget across 80% of its limit. */
    private fun checkAlerts(before: BudgetState, tx: TransactionEntity, newSplits: List<TransactionSplitEntity>) {
        val today = LocalDate.now()
        val after = before.transactions.filterNot { it.id == tx.id } + tx
        val splitsAfter = before.splits.filterNot { it.transactionId == tx.id } + newSplits.map { it.copy(transactionId = tx.id) }
        for (b in before.budgets) {
            if (b.categoryId != null && b.categoryId != tx.categoryId && newSplits.none { it.categoryId == b.categoryId }) continue
            val range = Budgeting.rangeFor(b, today, before.startDay, before.firstDay)
            val f0 = Budgeting.status(b, before.transactions, range, before.startDay, before.splits).fraction
            val st = Budgeting.status(b, after, range, before.startDay, splitsAfter)
            if (f0 < Budgeting.CLOSE_AT && st.fraction >= Budgeting.CLOSE_AT) {
                Notifier.budgetAlert(app, before.category(b.categoryId)?.name ?: app.getString(R.string.monthly_budget), (st.fraction * 100).toInt())
            }
        }
    }

    fun delete(tx: TransactionEntity) { viewModelScope.launch { repo.softDeleteTx(tx.id) } }
    fun restore(tx: TransactionEntity) { viewModelScope.launch { repo.restoreTx(tx.id) } }
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
        viewModelScope.launch { repo.reinsert(tx ?: return@launch, p) }
    }

    fun skipBill(bill: BillEntity, due: LocalDate) { viewModelScope.launch { repo.skipBill(bill, due, LocalDate.now()) } }
    fun clearPayment(p: BillPaymentEntity) { viewModelScope.launch { repo.clearPayment(p) } }
    fun saveTemplate(t: TemplateEntity) { viewModelScope.launch { repo.saveTemplate(t) } }

    /** Adds a template as a transaction dated today. [onAdded] gets the saved row so the caller can offer Undo. */
    fun quickAdd(t: TemplateEntity, onAdded: (TransactionEntity) -> Unit = {}) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                kind = t.kind, amountMinor = t.amountMinor, accountId = t.accountId, categoryId = t.categoryId,
                note = t.note, epochDay = LocalDate.now().toEpochDay(),
            )
            val saved = tx.copy(id = repo.save(tx))
            onAdded(saved)
            if (saved.kind == TxKind.EXPENSE && state.value.settings.budgetAlerts) checkAlerts(state.value, saved, emptyList())
        }
    }

    suspend fun quickAddById(id: Long): TemplateEntity? {
        val t = state.value.templates.firstOrNull { it.id == id } ?: return null
        quickAdd(t)
        return t
    }
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
