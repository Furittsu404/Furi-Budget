package app.furibudget.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BillScheduleTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun monthly(first: LocalDate, anchor: Int = first.dayOfMonth, end: LocalDate? = null, total: Int? = null) =
        BillSpec(Repeat.MONTHLY, 1, anchor, first, end, total)

    @Test fun monthlyKeepsAnchorAndNumbersFromOne() {
        val occ = BillSchedule.occurrences(monthly(d(2026, 1, 31)), d(2026, 1, 1), d(2026, 4, 30))
        assertEquals(listOf(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30)), occ.map { it.due })
        assertEquals(listOf(1, 2, 3, 4), occ.map { it.number })
    }

    @Test fun windowSkipsEarlierOnesButKeepsNumbering() {
        val occ = BillSchedule.occurrences(monthly(d(2026, 1, 5)), d(2026, 3, 1), d(2026, 4, 30))
        assertEquals(listOf(3, 4), occ.map { it.number })
    }

    @Test fun everyNGivesBiweeklyAndQuarterly() {
        val biweekly = BillSpec(Repeat.WEEKLY, 2, 1, d(2026, 11, 2))
        assertEquals(listOf(d(2026, 11, 2), d(2026, 11, 16), d(2026, 11, 30)), BillSchedule.occurrences(biweekly, d(2026, 11, 1), d(2026, 11, 30)).map { it.due })
        val quarterly = BillSpec(Repeat.MONTHLY, 3, 15, d(2026, 1, 15))
        assertEquals(listOf(d(2026, 1, 15), d(2026, 4, 15), d(2026, 7, 15)), BillSchedule.occurrences(quarterly, d(2026, 1, 1), d(2026, 8, 1)).map { it.due })
    }

    @Test fun instalmentsStopAfterTheirCount() {
        val spec = monthly(d(2026, 1, 10), total = 3)
        assertEquals(3, BillSchedule.occurrences(spec, d(2026, 1, 1), d(2030, 1, 1)).size)
        assertEquals(d(2026, 3, 10), BillSchedule.lastDue(spec))
    }

    @Test fun endDateStopsTheSeriesAndOneOffHasOneDate() {
        assertEquals(2, BillSchedule.occurrences(monthly(d(2026, 1, 10), end = d(2026, 2, 20)), d(2026, 1, 1), d(2027, 1, 1)).size)
        val once = BillSpec(Repeat.NONE, 1, 1, d(2026, 11, 30))
        assertEquals(1, BillSchedule.occurrences(once, d(2026, 1, 1), d(2027, 1, 1)).size)
        assertEquals(d(2026, 11, 30), BillSchedule.lastDue(once))
        assertNull(BillSchedule.lastDue(monthly(d(2026, 1, 10))))
    }

    @Test fun statusFollowsPaymentThenDate() {
        val today = d(2026, 11, 26)
        assertEquals(BillStatus.PAID, BillSchedule.statusOf(d(2026, 11, 1), PayState.PAID, today, 3))
        assertEquals(BillStatus.SKIPPED, BillSchedule.statusOf(d(2026, 11, 1), PayState.SKIPPED, today, 3))
        assertEquals(BillStatus.OVERDUE, BillSchedule.statusOf(d(2026, 11, 25), null, today, 3))
        assertEquals(BillStatus.DUE_TODAY, BillSchedule.statusOf(today, null, today, 3))
        assertEquals(BillStatus.DUE_SOON, BillSchedule.statusOf(d(2026, 11, 29), null, today, 3))
        assertEquals(BillStatus.UPCOMING, BillSchedule.statusOf(d(2026, 11, 30), null, today, 3))
        assertEquals(BillStatus.UPCOMING, BillSchedule.statusOf(d(2026, 11, 27), null, today, -1))
    }

    @Test fun monthlyCostNormalises() {
        assertEquals(1_000_00, BillSchedule.monthlyCost(1_000_00, Repeat.MONTHLY, 1))
        assertEquals(100_00, BillSchedule.monthlyCost(1_200_00, Repeat.YEARLY, 1))
        assertEquals(433_33, BillSchedule.monthlyCost(100_00, Repeat.WEEKLY, 1))
        assertEquals(500_00, BillSchedule.monthlyCost(1_500_00, Repeat.MONTHLY, 3))
    }
}
