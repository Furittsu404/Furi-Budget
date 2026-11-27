package app.furibudget.data

import androidx.room.withTransaction
import app.furibudget.core.BillSchedule
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val db: BudgetDatabase) {
    val allAccounts: Flow<List<AccountEntity>> = db.accounts().observeAll()
    val categories: Flow<List<CategoryEntity>> = db.categories().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactions().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgets().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goals().observeAll()
    val bills: Flow<List<BillEntity>> = db.bills().observeAll()
    val billPayments: Flow<List<BillPaymentEntity>> = db.billPayments().observeAll()
    val templates: Flow<List<TemplateEntity>> = db.templates().observeAll()
    val deletedTransactions: Flow<List<TransactionEntity>> = db.transactions().observeDeleted()
    val deletedBills: Flow<List<BillEntity>> = db.bills().observeDeleted()
    val deletedGoals: Flow<List<GoalEntity>> = db.goals().observeDeleted()

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

    /** Moves a transaction to Recently deleted. Restoring it brings it back exactly as it was, bill payment included. */
    suspend fun softDeleteTx(id: Long) = db.transactions().markDeleted(id, System.currentTimeMillis())
    suspend fun restoreTx(id: Long) = db.transactions().restoreDeleted(id)
    suspend fun deleteTxForever(tx: TransactionEntity) = db.transactions().delete(tx)

    /** Puts a transaction and its bill payment back after an unpay was undone. */
    suspend fun reinsert(tx: TransactionEntity, payment: BillPaymentEntity? = null) {
        db.withTransaction {
            db.transactions().insert(tx)
            if (payment != null) db.billPayments().insert(payment)
        }
    }

    /** Removes everything that has sat in Recently deleted for longer than [retentionMs]. */
    suspend fun purgeDeleted(now: Long, retentionMs: Long) {
        val before = now - retentionMs
        db.transactions().purge(before); db.bills().purge(before); db.goals().purge(before)
    }

    suspend fun saveBudget(b: BudgetEntity) = db.budgets().upsert(b)
    suspend fun deleteBudget(b: BudgetEntity) = db.budgets().delete(b)
    suspend fun saveGoal(g: GoalEntity) { if (g.id == 0L) db.goals().insert(g) else db.goals().update(g) }
    suspend fun deleteGoal(g: GoalEntity) = db.goals().markDeleted(g.id, System.currentTimeMillis())
    suspend fun restoreGoal(id: Long) = db.goals().restoreDeleted(id)
    suspend fun deleteGoalForever(g: GoalEntity) = db.goals().delete(g)

    suspend fun saveAccount(a: AccountEntity) { if (a.id == 0L) db.accounts().insert(a) else db.accounts().update(a) }
    suspend fun saveCategory(c: CategoryEntity) { if (c.id == 0L) db.categories().insert(c) else db.categories().update(c) }

    suspend fun saveTemplate(t: TemplateEntity) { db.templates().insert(t) }
    suspend fun deleteTemplate(t: TemplateEntity) = db.templates().delete(t)

    // ---- Bills

    suspend fun bill(id: Long) = db.bills().get(id)
    suspend fun activeBills() = db.bills().active()
    suspend fun saveBill(b: BillEntity): Long = if (b.id == 0L) db.bills().insert(b) else { db.bills().update(b); b.id }
    suspend fun deleteBill(b: BillEntity) = db.bills().markDeleted(b.id, System.currentTimeMillis())
    suspend fun restoreBill(id: Long) = db.bills().restoreDeleted(id)
    suspend fun deleteBillForever(b: BillEntity) = db.bills().delete(b)

    /**
     * Records one due date as paid: an expense (or income, or transfer, matching the bill) plus the payment row,
     * in one transaction. Returns null and does nothing if that date already has a payment, so a double tap cannot pay twice.
     */
    suspend fun payBill(bill: BillEntity, due: LocalDate, amountMinor: Long, accountId: Long, paidOn: LocalDate): BillPaymentEntity? = db.withTransaction {
        val existing = db.billPayments().find(bill.id, due.toEpochDay())
        if (existing != null) {
            // A payment whose expense sits in Recently deleted is stale: that date counts as unpaid, so allow paying it again.
            val tx = existing.transactionId?.let { db.transactions().get(it) }
            if (tx?.deletedAt == null) return@withTransaction null
            db.billPayments().delete(existing)
        }
        val txId = db.transactions().insert(
            TransactionEntity(
                kind = bill.kind, amountMinor = amountMinor, accountId = accountId,
                toAccountId = if (bill.kind == TxKind.TRANSFER) bill.toAccountId else null,
                categoryId = if (bill.kind == TxKind.TRANSFER) null else bill.categoryId,
                note = bill.name, epochDay = paidOn.toEpochDay(),
            ),
        )
        val payment = BillPaymentEntity(billId = bill.id, dueEpochDay = due.toEpochDay(), status = PayStatus.PAID, paidEpochDay = paidOn.toEpochDay(), amountMinor = amountMinor, transactionId = txId)
        payment.copy(id = db.billPayments().insert(payment))
    }

    /** Takes a paid date back: removes the expense it recorded and the payment. Returns what was removed for undo. */
    suspend fun unpay(payment: BillPaymentEntity): Pair<TransactionEntity?, BillPaymentEntity> = db.withTransaction {
        val tx = payment.transactionId?.let { db.transactions().get(it) }
        db.billPayments().delete(payment)
        if (tx != null) db.transactions().delete(tx)
        tx to payment
    }

    suspend fun skipBill(bill: BillEntity, due: LocalDate, today: LocalDate) {
        db.billPayments().insert(BillPaymentEntity(billId = bill.id, dueEpochDay = due.toEpochDay(), status = PayStatus.SKIPPED, paidEpochDay = today.toEpochDay()))
    }

    suspend fun clearPayment(p: BillPaymentEntity) = db.billPayments().delete(p)

    /**
     * Pays every auto-pay bill that has come due, including days missed while the app was closed (up to the look-back).
     * Safe to call any number of times: the unique (bill, due date) payment row stops a second payment.
     * Returns the bills paid, newest due date last, so a notification can say what happened.
     */
    suspend fun postAutoPay(today: LocalDate): List<Pair<BillEntity, LocalDate>> = db.withTransaction {
        val done = ArrayList<Pair<BillEntity, LocalDate>>()
        for (bill in db.bills().activeAutoPay()) {
            if (bill.amountMinor <= 0) continue
            val from = maxOf(LocalDate.ofEpochDay(bill.firstDueEpochDay), today.minusDays(BillSchedule.LOOKBACK_DAYS))
            for (occ in BillSchedule.occurrences(Bills.spec(bill), from, today)) {
                if (db.billPayments().find(bill.id, occ.due.toEpochDay()) != null) continue
                val txId = db.transactions().insert(
                    TransactionEntity(
                        kind = bill.kind, amountMinor = bill.amountMinor, accountId = bill.accountId,
                        toAccountId = if (bill.kind == TxKind.TRANSFER) bill.toAccountId else null,
                        categoryId = if (bill.kind == TxKind.TRANSFER) null else bill.categoryId,
                        note = bill.name, epochDay = occ.due.toEpochDay(),
                    ),
                )
                db.billPayments().insert(BillPaymentEntity(billId = bill.id, dueEpochDay = occ.due.toEpochDay(), status = PayStatus.PAID, paidEpochDay = occ.due.toEpochDay(), amountMinor = bill.amountMinor, transactionId = txId))
                done += bill to occ.due
            }
        }
        done
    }

    suspend fun allPayments() = db.billPayments().all()

    /** True the first time a reminder stage is claimed for a due date, false afterwards. */
    suspend fun claimAlert(billId: Long, due: LocalDate, stage: Int): Boolean = db.withTransaction {
        if (db.billAlerts().count(billId, due.toEpochDay(), stage) > 0) false
        else { db.billAlerts().insert(BillAlertEntity(billId, due.toEpochDay(), stage)); true }
    }

    suspend fun pruneAlerts(today: LocalDate) = db.billAlerts().prune(today.minusDays(BillSchedule.LOOKBACK_DAYS + 30).toEpochDay())

    // ---- Backup

    suspend fun export(theme: app.furibudget.ui.theme.ThemeSpec?, currency: String, startDay: Int): BackupFile = db.withTransaction {
        BackupFile(
            accounts = db.accounts().all(), categories = db.categories().all(), transactions = db.transactions().all(),
            budgets = db.budgets().all(), goals = db.goals().all(), bills = db.bills().all(), billPayments = db.billPayments().all(),
            templates = db.templates().all(), theme = theme, currency = currency, startDay = startDay,
        )
    }

    /** Replaces everything with the backup's contents, keeping ids so links between rows survive. */
    suspend fun replaceAll(f: BackupFile) = db.withTransaction {
        db.billPayments().deleteAll(); db.transactions().deleteAll(); db.bills().deleteAll(); db.templates().deleteAll()
        db.budgets().deleteAll(); db.goals().deleteAll(); db.categories().deleteAll(); db.accounts().deleteAll()
        db.accounts().insertAll(f.accounts); db.categories().insertAll(f.categories)
        db.transactions().insertAll(f.transactions); db.budgets().insertAll(f.budgets)
        db.goals().insertAll(f.goals); db.bills().insertAll(f.bills); db.billPayments().insertAll(f.billPayments)
        db.templates().insertAll(f.templates)
    }

    /** Clears every row, including the seeded defaults. */
    suspend fun wipe() = replaceAll(BackupFile())
}
