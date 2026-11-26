package app.furibudget

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.data.BudgetEntity
import app.furibudget.data.Budgeting
import app.furibudget.data.GoalEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetingTest {
    private val nov = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
    private fun exp(month: Int, day: Int, amt: Long, cat: Long) = TransactionEntity(
        kind = TxKind.EXPENSE, amountMinor = amt, accountId = 1, categoryId = cat, epochDay = LocalDate.of(2026, month, day).toEpochDay(),
    )

    @Test fun levels() {
        val b = BudgetEntity(categoryId = 1, limitMinor = 1000)
        assertEquals(Budgeting.Level.OK, Budgeting.status(b, listOf(exp(11, 2, 500, 1)), nov).level)
        assertEquals(Budgeting.Level.CLOSE, Budgeting.status(b, listOf(exp(11, 2, 850, 1)), nov).level)
        assertEquals(Budgeting.Level.OVER, Budgeting.status(b, listOf(exp(11, 2, 1001, 1)), nov).level)
        assertEquals(Budgeting.Level.CLOSE, Budgeting.status(b, listOf(exp(11, 2, 1000, 1)), nov).level)
    }

    @Test fun overallCountsEveryCategory() {
        val b = BudgetEntity(categoryId = null, limitMinor = 1000)
        val s = Budgeting.status(b, listOf(exp(11, 2, 300, 1), exp(11, 3, 200, 2), exp(10, 3, 999, 2)), nov)
        assertEquals(Money(500), s.spent)
        assertEquals(Money(500), s.left)
    }

    @Test fun rolloverAddsOnlyUnspent() {
        val b = BudgetEntity(categoryId = 1, limitMinor = 1000, rollover = true)
        assertEquals(Money(1400), Budgeting.status(b, listOf(exp(10, 5, 600, 1)), nov).limit)
        assertEquals(Money(1000), Budgeting.status(b, listOf(exp(10, 5, 1500, 1)), nov).limit)
    }

    @Test fun goalPlan() {
        val today = LocalDate.of(2026, 11, 26)
        val g = GoalEntity(name = "Trip", targetMinor = 120_000, savedMinor = 20_000, deadlineEpochDay = LocalDate.of(2027, 3, 15).toEpochDay(), colorArgb = 0)
        val p = Budgeting.plan(g, today)
        assertEquals(Money(100_000), p.remaining)
        assertEquals(Money(25_000), p.monthlyNeeded)
        assertTrue(Budgeting.plan(g.copy(savedMinor = 120_000), today).reached)
        assertNull(Budgeting.plan(g.copy(deadlineEpochDay = null), today).monthlyNeeded)
        assertTrue(Budgeting.plan(g.copy(deadlineEpochDay = today.minusDays(1).toEpochDay()), today).overdue)
    }
}
