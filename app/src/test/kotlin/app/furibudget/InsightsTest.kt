package app.furibudget

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.data.Insights
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsTest {
    private val nov = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
    private fun exp(day: Int, amt: Long, cat: Long? = 1, month: Int = 11) = TransactionEntity(
        kind = TxKind.EXPENSE, amountMinor = amt, accountId = 1, categoryId = cat,
        epochDay = LocalDate.of(2026, month, day).toEpochDay(),
    )

    @Test fun spendByCategorySortsAndGroups() {
        val r = Insights.spendByCategory(listOf(exp(1, 100, 1), exp(2, 500, 2), exp(3, 50, 1), exp(4, 7, null)), nov)
        assertEquals(listOf(2L, 1L, null), r.map { it.categoryId })
        assertEquals(Money(150), r[1].amount)
    }

    @Test fun cumulativeStopsAtToday() {
        val c = Insights.cumulativeSpend(listOf(exp(2, 100), exp(5, 50)), nov, upTo = LocalDate.of(2026, 11, 4))
        assertEquals(30, c.size)
        assertEquals(0L, c[0])
        assertEquals(100L, c[1])
        assertEquals(100L, c[3])
        assertEquals(100L, c[29])
    }

    @Test fun monthBarsOldestFirst() {
        val bars = Insights.monthBars(listOf(exp(3, 70, month = 10), exp(3, 30)), LocalDate.of(2026, 11, 26), 3)
        assertEquals(3, bars.size)
        assertEquals(LocalDate.of(2026, 9, 1), bars[0].range.start)
        assertEquals(Money(70), bars[1].expense)
        assertEquals(Money(30), bars[2].expense)
    }
}
