package app.furibudget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM accounts ORDER BY id") suspend fun all(): List<AccountEntity>
    @Query("DELETE FROM accounts") suspend fun deleteAll()
    @Insert suspend fun insertAll(accounts: List<AccountEntity>)
    @Insert suspend fun insert(account: AccountEntity): Long
    @Update suspend fun update(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<CategoryEntity>>

    /** Includes archived ones so old transactions can still show their category. */
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert suspend fun insert(category: CategoryEntity): Long
    @Query("SELECT * FROM categories ORDER BY id") suspend fun all(): List<CategoryEntity>
    @Query("DELETE FROM categories") suspend fun deleteAll()
    @Insert suspend fun insertAll(categories: List<CategoryEntity>)
    @Update suspend fun update(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY id") suspend fun all(): List<TransactionEntity>
    @Query("DELETE FROM transactions") suspend fun deleteAll()
    @Insert suspend fun insertAll(txs: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Insert suspend fun insert(tx: TransactionEntity): Long
    @Update suspend fun update(tx: TransactionEntity)
    @Delete suspend fun delete(tx: TransactionEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets") suspend fun all(): List<BudgetEntity>
    @Query("DELETE FROM budgets") suspend fun deleteAll()
    @Insert suspend fun insertAll(b: List<BudgetEntity>)
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun upsert(b: BudgetEntity)
    @Delete suspend fun delete(b: BudgetEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals") suspend fun all(): List<GoalEntity>
    @Query("DELETE FROM goals") suspend fun deleteAll()
    @Insert suspend fun insertAll(g: List<GoalEntity>)
    @Insert suspend fun insert(g: GoalEntity): Long
    @Update suspend fun update(g: GoalEntity)
    @Delete suspend fun delete(g: GoalEntity)
}

@Dao
interface RecurrenceDao {
    @Query("SELECT * FROM recurrences ORDER BY nextDueEpochDay")
    fun observeAll(): Flow<List<RecurrenceEntity>>

    @Query("SELECT * FROM recurrences WHERE active = 1 AND nextDueEpochDay <= :today")
    suspend fun due(today: Long): List<RecurrenceEntity>

    @Query("SELECT * FROM recurrences ORDER BY id") suspend fun all(): List<RecurrenceEntity>
    @Query("DELETE FROM recurrences") suspend fun deleteAll()
    @Insert suspend fun insertAll(r: List<RecurrenceEntity>)
    @Insert suspend fun insert(r: RecurrenceEntity): Long
    @Update suspend fun update(r: RecurrenceEntity)
    @Delete suspend fun delete(r: RecurrenceEntity)
}
