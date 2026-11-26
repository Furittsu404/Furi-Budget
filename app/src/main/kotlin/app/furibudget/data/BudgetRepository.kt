package app.furibudget.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val db: BudgetDatabase) {
    val allAccounts: Flow<List<AccountEntity>> = db.accounts().observeAll()
    val categories: Flow<List<CategoryEntity>> = db.categories().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactions().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgets().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goals().observeAll()
    val recurrences: Flow<List<RecurrenceEntity>> = db.recurrences().observeAll()

    suspend fun seedIfEmpty() {
        db.withTransaction {
            if (db.accounts().count() == 0) {
                Seed.accounts().forEach { db.accounts().insert(it) }
                db.categories().insertAll(Seed.categories())
            }
        }
    }

    suspend fun transaction(id: Long) = db.transactions().get(id)

    suspend fun save(tx: TransactionEntity): Long =
        if (tx.id == 0L) db.transactions().insert(tx) else { db.transactions().update(tx); tx.id }

    suspend fun delete(tx: TransactionEntity) = db.transactions().delete(tx)

    /** Puts a deleted transaction back with its old id (for the undo snackbar). */
    suspend fun restore(tx: TransactionEntity) { db.transactions().insert(tx) }

    suspend fun saveBudget(b: BudgetEntity) = db.budgets().upsert(b)
    suspend fun deleteBudget(b: BudgetEntity) = db.budgets().delete(b)
    suspend fun saveGoal(g: GoalEntity) { if (g.id == 0L) db.goals().insert(g) else db.goals().update(g) }
    suspend fun deleteGoal(g: GoalEntity) = db.goals().delete(g)

    suspend fun saveAccount(a: AccountEntity) { if (a.id == 0L) db.accounts().insert(a) else db.accounts().update(a) }
    suspend fun saveCategory(c: CategoryEntity) { if (c.id == 0L) db.categories().insert(c) else db.categories().update(c) }

    suspend fun saveRecurrence(r: RecurrenceEntity) { if (r.id == 0L) db.recurrences().insert(r) else db.recurrences().update(r) }
    suspend fun deleteRecurrence(r: RecurrenceEntity) = db.recurrences().delete(r)

    /**
     * Posts every occurrence that has come due, catching up on missed days, and moves each series forward.
     * Safe to call any number of times: the series' nextDue advances in the same database transaction.
     * Returns how many transactions were added.
     */
    suspend fun postDueRecurrences(today: java.time.LocalDate): Int = db.withTransaction {
        var added = 0
        for (r in db.recurrences().due(today.toEpochDay())) {
            val end = r.endEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
            val dates = app.furibudget.core.Recurrence.dueDates(java.time.LocalDate.ofEpochDay(r.nextDueEpochDay), r.repeat, r.anchorDay, today, end)
            for (d in dates) {
                db.transactions().insert(
                    TransactionEntity(kind = r.kind, amountMinor = r.amountMinor, accountId = r.accountId, toAccountId = r.toAccountId, categoryId = r.categoryId, note = r.note, epochDay = d.toEpochDay()),
                )
                added++
            }
            val last = dates.lastOrNull()
            val next = if (last != null) app.furibudget.core.Recurrence.next(last, r.repeat, r.anchorDay) else java.time.LocalDate.ofEpochDay(r.nextDueEpochDay)
            val finished = end != null && next.isAfter(end)
            db.recurrences().update(r.copy(nextDueEpochDay = next.toEpochDay(), active = r.active && !finished))
        }
        added
    }

    suspend fun export(theme: app.furibudget.ui.theme.ThemeSpec?, currency: String, startDay: Int): BackupFile = db.withTransaction {
        BackupFile(
            accounts = db.accounts().all(), categories = db.categories().all(), transactions = db.transactions().all(),
            budgets = db.budgets().all(), goals = db.goals().all(), recurrences = db.recurrences().all(),
            theme = theme, currency = currency, startDay = startDay,
        )
    }

    /** Replaces everything with the backup's contents, keeping ids so links between rows survive. */
    suspend fun replaceAll(f: BackupFile) = db.withTransaction {
        db.transactions().deleteAll(); db.recurrences().deleteAll(); db.budgets().deleteAll()
        db.goals().deleteAll(); db.categories().deleteAll(); db.accounts().deleteAll()
        db.accounts().insertAll(f.accounts); db.categories().insertAll(f.categories)
        db.transactions().insertAll(f.transactions); db.budgets().insertAll(f.budgets)
        db.goals().insertAll(f.goals); db.recurrences().insertAll(f.recurrences)
    }

    /** Clears every row, including the seeded defaults. */
    suspend fun wipe() = replaceAll(BackupFile())
}
