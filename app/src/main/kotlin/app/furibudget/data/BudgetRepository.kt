package app.furibudget.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val db: BudgetDatabase) {
    val accounts: Flow<List<AccountEntity>> = db.accounts().observeActive()
    val categories: Flow<List<CategoryEntity>> = db.categories().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactions().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgets().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goals().observeAll()

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
}
