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
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY epochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET deletedAt = :at WHERE id = :id") suspend fun markDeleted(id: Long, at: Long)
    @Query("UPDATE transactions SET deletedAt = NULL WHERE id = :id") suspend fun restoreDeleted(id: Long)
    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL AND deletedAt < :before") suspend fun purge(before: Long)

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
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET deletedAt = :at WHERE id = :id") suspend fun markDeleted(id: Long, at: Long)
    @Query("UPDATE goals SET deletedAt = NULL WHERE id = :id") suspend fun restoreDeleted(id: Long)
    @Query("DELETE FROM goals WHERE deletedAt IS NOT NULL AND deletedAt < :before") suspend fun purge(before: Long)

    @Query("SELECT * FROM goals") suspend fun all(): List<GoalEntity>
    @Query("DELETE FROM goals") suspend fun deleteAll()
    @Insert suspend fun insertAll(g: List<GoalEntity>)
    @Insert suspend fun insert(g: GoalEntity): Long
    @Update suspend fun update(g: GoalEntity)
    @Delete suspend fun delete(g: GoalEntity)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<BillEntity>>

    @Query("UPDATE bills SET deletedAt = :at WHERE id = :id") suspend fun markDeleted(id: Long, at: Long)
    @Query("UPDATE bills SET deletedAt = NULL WHERE id = :id") suspend fun restoreDeleted(id: Long)
    @Query("DELETE FROM bills WHERE deletedAt IS NOT NULL AND deletedAt < :before") suspend fun purge(before: Long)

    @Query("SELECT * FROM bills WHERE id = :id") suspend fun get(id: Long): BillEntity?
    @Query("SELECT * FROM bills WHERE active = 1 AND autoPay = 1 AND deletedAt IS NULL") suspend fun activeAutoPay(): List<BillEntity>
    @Query("SELECT * FROM bills WHERE active = 1 AND deletedAt IS NULL") suspend fun active(): List<BillEntity>
    @Query("SELECT * FROM bills ORDER BY id") suspend fun all(): List<BillEntity>
    @Query("DELETE FROM bills") suspend fun deleteAll()
    @Insert suspend fun insertAll(b: List<BillEntity>)
    @Insert suspend fun insert(b: BillEntity): Long
    @Update suspend fun update(b: BillEntity)
    @Delete suspend fun delete(b: BillEntity)
}

@Dao
interface BillPaymentDao {
    // A payment whose expense was moved to Recently deleted does not count: that date is unpaid again until it is restored.
    @Query("SELECT p.* FROM bill_payments p LEFT JOIN transactions t ON t.id = p.transactionId WHERE t.id IS NULL OR t.deletedAt IS NULL")
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

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<TagEntity>>
    @Query("SELECT * FROM tags ORDER BY id") suspend fun all(): List<TagEntity>
    @Query("DELETE FROM tags") suspend fun deleteAll()
    @Insert suspend fun insertAll(t: List<TagEntity>)
    @Insert suspend fun insert(t: TagEntity): Long
    @Delete suspend fun delete(t: TagEntity)
}

@Dao
interface TransactionTagDao {
    @Query("SELECT * FROM transaction_tags") fun observeAll(): Flow<List<TransactionTagEntity>>
    @Query("SELECT * FROM transaction_tags") suspend fun all(): List<TransactionTagEntity>
    @Query("DELETE FROM transaction_tags WHERE transactionId = :txId") suspend fun deleteFor(txId: Long)
    @Query("DELETE FROM transaction_tags") suspend fun deleteAll()
    @Insert suspend fun insertAll(t: List<TransactionTagEntity>)
}

@Dao
interface SplitDao {
    @Query("SELECT * FROM transaction_splits") fun observeAll(): Flow<List<TransactionSplitEntity>>
    @Query("SELECT * FROM transaction_splits ORDER BY id") suspend fun all(): List<TransactionSplitEntity>
    @Query("DELETE FROM transaction_splits WHERE transactionId = :txId") suspend fun deleteFor(txId: Long)
    @Query("DELETE FROM transaction_splits") suspend fun deleteAll()
    @Insert suspend fun insertAll(s: List<TransactionSplitEntity>)
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM people WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM people ORDER BY id") suspend fun all(): List<PersonEntity>
    @Query("SELECT * FROM people WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC") fun observeDeleted(): Flow<List<PersonEntity>>
    @Query("DELETE FROM people") suspend fun deleteAll()
    @Insert suspend fun insert(p: PersonEntity): Long
    @Insert suspend fun insertAll(p: List<PersonEntity>)
    @Update suspend fun update(p: PersonEntity)
    @Query("UPDATE people SET deletedAt = :at WHERE id = :id") suspend fun markDeleted(id: Long, at: Long)
    @Query("UPDATE people SET deletedAt = NULL WHERE id = :id") suspend fun restoreDeleted(id: Long)
    @Query("DELETE FROM people WHERE id = :id") suspend fun deleteForever(id: Long)
    @Query("DELETE FROM people WHERE deletedAt IS NOT NULL AND deletedAt < :before") suspend fun purge(before: Long)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE deletedAt IS NULL ORDER BY openedEpochDay DESC, id DESC") fun observeAll(): Flow<List<LoanEntity>>
    @Query("SELECT * FROM loans ORDER BY id") suspend fun all(): List<LoanEntity>
    @Query("SELECT * FROM loans WHERE id = :id") suspend fun get(id: Long): LoanEntity?
    @Query("DELETE FROM loans") suspend fun deleteAll()
    @Insert suspend fun insert(l: LoanEntity): Long
    @Insert suspend fun insertAll(l: List<LoanEntity>)
    @Query("UPDATE loans SET deletedAt = :at WHERE personId = :personId") suspend fun markPersonDeleted(personId: Long, at: Long)
    @Query("UPDATE loans SET deletedAt = NULL WHERE personId = :personId") suspend fun restorePerson(personId: Long)
}
