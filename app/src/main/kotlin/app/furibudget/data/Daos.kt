package app.furibudget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    @androidx.room.Delete suspend fun delete(account: AccountEntity)
    @Query("UPDATE accounts SET sortOrder = :order WHERE id = :id") suspend fun setOrder(id: Long, order: Int)
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
    @Query("UPDATE categories SET sortOrder = :order WHERE id = :id") suspend fun setOrder(id: Long, order: Int)
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
    @Query("UPDATE transactions SET accountId = :to WHERE accountId = :from") suspend fun repointAccount(from: Long, to: Long)
    @Query("UPDATE transactions SET toAccountId = :to WHERE toAccountId = :from") suspend fun repointToAccount(from: Long, to: Long)
    @Query("DELETE FROM transactions WHERE kind = 'TRANSFER' AND accountId = toAccountId") suspend fun deleteSelfTransfers()
    @Query("DELETE FROM transactions WHERE accountId = :id OR (kind = 'TRANSFER' AND toAccountId = :id)") suspend fun deleteForAccount(id: Long)
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

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): GoalEntity?

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
    @Query("UPDATE bills SET accountId = :to WHERE accountId = :from") suspend fun repointAccount(from: Long, to: Long)
    @Query("UPDATE bills SET toAccountId = :to WHERE toAccountId = :from") suspend fun repointToAccount(from: Long, to: Long)
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
    @Query("UPDATE templates SET accountId = :to WHERE accountId = :from") suspend fun repointAccount(from: Long, to: Long)
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

@Dao
interface FundDao {
    @Query("SELECT * FROM funds WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<FundEntity>>
    @Query("SELECT * FROM funds WHERE deletedAt IS NULL") suspend fun active(): List<FundEntity>
    @Query("SELECT * FROM funds WHERE id = :id") suspend fun get(id: Long): FundEntity?
    @Query("SELECT * FROM funds ORDER BY id") suspend fun all(): List<FundEntity>
    @Query("SELECT * FROM funds WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC") fun observeDeleted(): Flow<List<FundEntity>>
    @Query("DELETE FROM funds") suspend fun deleteAll()
    @Insert suspend fun insert(f: FundEntity): Long
    @Insert suspend fun insertAll(f: List<FundEntity>)
    @Update suspend fun update(f: FundEntity)
    @Query("UPDATE funds SET deletedAt = :at WHERE id = :id") suspend fun markDeleted(id: Long, at: Long)
    @Query("UPDATE funds SET deletedAt = NULL WHERE id = :id") suspend fun restoreDeleted(id: Long)
    @Query("DELETE FROM funds WHERE id = :id") suspend fun deleteForever(id: Long)
    @Query("DELETE FROM funds WHERE deletedAt IS NOT NULL AND deletedAt < :before") suspend fun purge(before: Long)
    @Query("UPDATE funds SET accountId = :to WHERE accountId = :from OR accountId IS NULL") suspend fun repointAccount(from: Long, to: Long)
}

@Dao
interface FundMoveDao {
    @Query("SELECT * FROM fund_moves") fun observeAll(): Flow<List<FundMoveEntity>>
    @Query("SELECT * FROM fund_moves ORDER BY id") suspend fun all(): List<FundMoveEntity>
    @Query("SELECT * FROM fund_moves WHERE fundId = :fundId") suspend fun forFund(fundId: Long): List<FundMoveEntity>
    /** What is in a goal right now: moves tied to a deleted transaction do not count. */
    @Query("SELECT COALESCE(SUM(m.amountMinor), 0) FROM fund_moves m LEFT JOIN transactions t ON t.id = m.transactionId WHERE m.fundId = :fundId AND (m.transactionId IS NULL OR (t.id IS NOT NULL AND t.deletedAt IS NULL))")
    suspend fun saved(fundId: Long): Long
    @Query("DELETE FROM fund_moves") suspend fun deleteAll()
    @Query("DELETE FROM fund_moves WHERE transactionId = :txId") suspend fun deleteForTx(txId: Long)
    @Insert suspend fun insertAll(m: List<FundMoveEntity>)
    @Insert suspend fun insert(m: FundMoveEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(m: FundMoveEntity): Long
}

@Dao
interface RateDao {
    @Query("SELECT * FROM rates ORDER BY code") fun observeAll(): Flow<List<RateEntity>>
    @Query("SELECT * FROM rates ORDER BY code") suspend fun all(): List<RateEntity>
    @Query("DELETE FROM rates") suspend fun deleteAll()
    @Query("DELETE FROM rates WHERE manual = 0") suspend fun deleteOnline()
    @Query("DELETE FROM rates WHERE code = :code") suspend fun delete(code: String)
    @Insert suspend fun insertAll(r: List<RateEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(r: RateEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(r: RateEntity): Long
}
