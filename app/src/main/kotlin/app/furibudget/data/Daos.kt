package app.furibudget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

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
    @Insert suspend fun insertAll(categories: List<CategoryEntity>)
    @Update suspend fun update(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

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

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun upsert(b: BudgetEntity)
    @Delete suspend fun delete(b: BudgetEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert suspend fun insert(g: GoalEntity): Long
    @Update suspend fun update(g: GoalEntity)
    @Delete suspend fun delete(g: GoalEntity)
}
