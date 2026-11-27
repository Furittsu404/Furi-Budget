package app.furibudget

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.data.Budgeting
import app.furibudget.data.Insights
import app.furibudget.data.Ledger
import app.furibudget.data.Spending
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TransactionSplitEntity
import app.furibudget.data.TransactionTagEntity
import app.furibudget.data.TxKind
import app.furibudget.data.TxRole
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingTest {
    private val nov = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
    private val day = LocalDate.of(2026, 11, 10).toEpochDay()
    private fun exp(id: Long, amt: Long, cat: Long? = null, role: TxRole = TxRole.NORMAL, deleted: Long? = null) =
        TransactionEntity(id = id, kind = TxKind.EXPENSE, amountMinor = amt, accountId = 1, categoryId = cat, epochDay = day, role = role, deletedAt = deleted)

    @Test fun splitSlicesReplaceTheParentCategory() {
        val txs = listOf(exp(1, 1000), exp(2, 300, 5))
        val splits = listOf(TransactionSplitEntity(transactionId = 1, categoryId = 7, amountMinor = 600), TransactionSplitEntity(transactionId = 1, categoryId = 8, amountMinor = 400))
        val r = Insights.spendByCategory(txs, nov, splits)
        assertEquals(mapOf<Long?, Long>(7L to 600, 8L to 400, 5L to 300), r.associate { it.categoryId to it.amount.minor })
        assertEquals(Money(1300), Money(r.sumOf { it.amount.minor }))
    }

    @Test fun budgetCountsOnlyTheMatchingSlice() {
        val txs = listOf(exp(1, 1000))
        val splits = listOf(TransactionSplitEntity(transactionId = 1, categoryId = 7, amountMinor = 600), TransactionSplitEntity(transactionId = 1, categoryId = 8, amountMinor = 400))
        val b = app.furibudget.data.BudgetEntity(categoryId = 7, limitMinor = 1000)
        assertEquals(Money(600), Budgeting.status(b, txs, nov, 1, splits).spent)
    }

    @Test fun nonNormalRolesAndDeletedRowsAreNotSpending() {
        val txs = listOf(exp(1, 500), exp(2, 900, role = TxRole.LOAN), exp(3, 100, role = TxRole.ADJUSTMENT), exp(4, 700, deleted = 5L))
        assertEquals(Money(500), Ledger.totals(txs.filter { it.deletedAt == null }, nov).expense)
        assertEquals(500L, Insights.spendByCategory(txs, nov).sumOf { it.amount.minor })
        assertEquals(1, Spending.lines(txs, emptyList()).size)
    }

    @Test fun spendByTagCountsATransactionUnderEachTag() {
        val txs = listOf(exp(1, 400), exp(2, 100))
        val links = listOf(TransactionTagEntity(1, 10), TransactionTagEntity(1, 11), TransactionTagEntity(2, 10))
        val r = Insights.spendByTag(txs, nov, links).associate { it.first to it.second.minor }
        assertEquals(mapOf(10L to 500L, 11L to 400L), r)
    }

    @Test fun balancedNeedsAnExactSum() {
        val s = listOf(TransactionSplitEntity(transactionId = 1, amountMinor = 60), TransactionSplitEntity(transactionId = 1, amountMinor = 40))
        assertTrue(Spending.balanced(100, s))
        assertFalse(Spending.balanced(101, s))
        assertFalse(Spending.balanced(0, emptyList()))
    }
}
