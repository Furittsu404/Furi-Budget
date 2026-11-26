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
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id") suspend fun get(id: Long): BillEntity?
    @Query("SELECT * FROM bills WHERE active = 1 AND autoPay = 1") suspend fun activeAutoPay(): List<BillEntity>
    @Query("SELECT * FROM bills WHERE active = 1") suspend fun active(): List<BillEntity>
    @Query("SELECT * FROM bills ORDER BY id") suspend fun all(): List<BillEntity>
    @Query("DELETE FROM bills") suspend fun deleteAll()
    @Insert suspend fun insertAll(b: List<BillEntity>)
    @Insert suspend fun insert(b: BillEntity): Long
    @Update suspend fun update(b: BillEntity)
    @Delete suspend fun delete(b: BillEntity)
}

@Dao
interface BillPaymentDao {
    @Query("SELECT * FROM bill_payments")
    fun observeAll(): Flow<List<BillPaymentEntity>>

    @Query("SELECT * FROM bill_payments") suspend fun all(): List<BillPaymentEntity>
    @Query("SELECT * FROM bill_payments WHERE billId = :billId AND dueEpochDay = :due") suspend fun find(billId: Long, due: Long): BillPaymentEntity?
    @Query("SELECT * FROM bill_payments WHERE transactionId = :txId") suspend fun byTransaction(txId: Long): BillPaymentEntity?
    @Query("DELETE FROM bill_payments") suspend fun deleteAll()
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE) suspend fun insert(p: BillPaymentEntity): Long
    @Insert suspend fun insertAll(p: List<BillPaymentEntity>)
    @Delete suspend fun delete(p: BillPaymentEntity)
}

@Dao
interface BillAlertDao {
    @Query("SELECT COUNT(*) FROM bill_alerts WHERE billId = :billId AND dueEpochDay = :due AND stage = :stage")
    suspend fun count(billId: Long, due: Long, stage: Int): Int

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE) suspend fun insert(a: BillAlertEntity)
    @Query("DELETE FROM bill_alerts WHERE dueEpochDay < :before") suspend fun prune(before: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY id") suspend fun all(): List<TemplateEntity>
    @Query("DELETE FROM templates") suspend fun deleteAll()
    @Insert suspend fun insertAll(t: List<TemplateEntity>)
    @Insert suspend fun insert(t: TemplateEntity): Long
    @Delete suspend fun delete(t: TemplateEntity)
}
