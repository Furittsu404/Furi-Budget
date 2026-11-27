package app.furibudget

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.data.BudgetEntity
import app.furibudget.data.Budgeting
import app.furibudget.data.FundEntity
import app.furibudget.data.FundStatus
import app.furibudget.data.Funds
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

    @Test fun weeklyRolloverUsesPreviousWeek() {
        val week = DateRange(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29))
        val b = BudgetEntity(categoryId = 1, limitMinor = 1000, rollover = true, period = app.furibudget.data.BudgetPeriod.WEEK)
        val s = Budgeting.status(b, listOf(exp(11, 18, 400, 1), exp(11, 24, 100, 1)), week)
        assertEquals(Money(1600), s.limit)
        assertEquals(Money(100), s.spent)
    }

    @Test fun rangeForPicksPeriod() {
        val today = LocalDate.of(2026, 11, 26)
        val monthly = BudgetEntity(limitMinor = 1)
        val weekly = BudgetEntity(limitMinor = 1, period = app.furibudget.data.BudgetPeriod.WEEK)
        assertEquals(LocalDate.of(2026, 11, 1), Budgeting.rangeFor(monthly, today, 1, java.time.DayOfWeek.MONDAY).start)
        assertEquals(LocalDate.of(2026, 11, 23), Budgeting.rangeFor(weekly, today, 1, java.time.DayOfWeek.MONDAY).start)
    }

    @Test fun goalPlan() {
        val today = LocalDate.of(2026, 11, 26)
        val fund = FundEntity(name = "Trip", colorArgb = 0, targetMinor = 120_000, deadlineEpochDay = LocalDate.of(2027, 3, 15).toEpochDay())
        val p = Funds.plan(FundStatus(fund, 20_000), today)
        assertEquals(100_000L, p.remainingMinor)
        assertEquals(25_000L, p.monthlyNeededMinor)
        assertTrue(Funds.plan(FundStatus(fund, 120_000), today).reached)
        assertNull(Funds.plan(FundStatus(fund.copy(deadlineEpochDay = null), 20_000), today).monthlyNeededMinor)
        assertTrue(Funds.plan(FundStatus(fund.copy(deadlineEpochDay = today.minusDays(1).toEpochDay()), 20_000), today).overdue)
        assertNull(Funds.plan(FundStatus(fund.copy(targetMinor = null), 20_000), today).monthlyNeededMinor)
    }
}
