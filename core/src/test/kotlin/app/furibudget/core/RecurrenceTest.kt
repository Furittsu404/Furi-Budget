package app.furibudget.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurrenceTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test fun monthlyKeepsAnchorAcrossShortMonths() {
        val jan31 = d(2026, 1, 31)
        val feb = Recurrence.next(jan31, Repeat.MONTHLY, 31)
        assertEquals(d(2026, 2, 28), feb)
        assertEquals(d(2026, 3, 31), Recurrence.next(feb, Repeat.MONTHLY, 31))
    }

    @Test fun yearlyHandlesLeapDay() {
        assertEquals(d(2027, 2, 28), Recurrence.next(d(2026, 2, 28), Repeat.YEARLY, 29))
    }

    @Test fun catchUpListsEveryMissedOccurrence() {
        val due = Recurrence.dueDates(d(2026, 11, 1), Repeat.WEEKLY, 1, through = d(2026, 11, 26))
        assertEquals(listOf(d(2026, 11, 1), d(2026, 11, 8), d(2026, 11, 15), d(2026, 11, 22)), due)
    }

    @Test fun respectsEndAndNone() {
        assertEquals(2, Recurrence.dueDates(d(2026, 11, 1), Repeat.DAILY, 1, d(2026, 11, 30), end = d(2026, 11, 2)).size)
        assertEquals(emptyList(), Recurrence.dueDates(d(2026, 11, 1), Repeat.NONE, 1, d(2026, 11, 30)))
    }

    @Test fun nothingDueInFuture() {
        assertEquals(emptyList(), Recurrence.dueDates(d(2026, 12, 1), Repeat.MONTHLY, 1, d(2026, 11, 26)))
    }
}
