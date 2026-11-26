package app.furibudget

import app.furibudget.core.BillStatus
import app.furibudget.core.Repeat
import app.furibudget.data.BillEntity
import app.furibudget.data.BillOccurrence
import app.furibudget.data.BillPaymentEntity
import app.furibudget.data.BillType
import app.furibudget.data.Bills
import app.furibudget.data.PayStatus
import app.furibudget.data.TxKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillsTest {
    private val today = LocalDate.of(2026, 11, 26)
    private fun bill(due: LocalDate, remind: Int = 3, id: Long = 1, repeat: Repeat = Repeat.MONTHLY) = BillEntity(
        id = id, name = "Rent", kind = TxKind.EXPENSE, type = BillType.BILL, amountMinor = 1000, accountId = 1,
        repeat = repeat, anchorDay = due.dayOfMonth, firstDueEpochDay = due.toEpochDay(), remindDaysBefore = remind, colorArgb = 0,
    )
    private fun occ(b: BillEntity, due: LocalDate, paid: Boolean = false): BillOccurrence {
        val o = Bills.occurrences(listOf(b), if (paid) listOf(BillPaymentEntity(1, b.id, due.toEpochDay(), PayStatus.PAID, due.toEpochDay(), 1000)) else emptyList(), due, due, today)
        return o.single()
    }

    @Test fun occurrencesCarryPaymentStatus() {
        val b = bill(today.plusDays(2))
        assertEquals(BillStatus.DUE_SOON, occ(b, today.plusDays(2)).status)
        assertEquals(BillStatus.PAID, occ(b, today.plusDays(2), paid = true).status)
    }

    @Test fun overdueListsUnpaidPastDatesOnly() {
        val b = bill(today.minusDays(40))
        val payments = listOf(BillPaymentEntity(1, b.id, today.minusDays(40).toEpochDay(), PayStatus.PAID, today.minusDays(40).toEpochDay(), 1000))
        // First date paid; the next one (about 10 days ago) is still open.
        val overdue = Bills.overdue(listOf(b), payments, today)
        assertEquals(1, overdue.size)
        assertEquals(BillStatus.OVERDUE, overdue[0].status)
    }

    @Test fun inactiveBillsProduceNothing() {
        assertTrue(Bills.occurrences(listOf(bill(today).copy(active = false)), emptyList(), today.minusDays(5), today.plusDays(5), today).isEmpty())
    }

    @Test fun reminderStages() {
        val b = bill(today.plusDays(2), remind = 3)
        assertEquals(0, BillNotifier.stageFor(occ(b, today.plusDays(2)), today))
        assertEquals(1, BillNotifier.stageFor(occ(bill(today), today), today))
        assertEquals(2, BillNotifier.stageFor(occ(bill(today.minusDays(1)), today.minusDays(1)), today))
        assertEquals(3, BillNotifier.stageFor(occ(bill(today.minusDays(9)), today.minusDays(9)), today))
        assertNull(BillNotifier.stageFor(occ(bill(today.plusDays(6)), today.plusDays(6)), today))
        assertNull(BillNotifier.stageFor(occ(bill(today.plusDays(2), remind = -1), today.plusDays(2)), today))
        assertNull(BillNotifier.stageFor(occ(b, today.plusDays(2), paid = true), today))
    }
}
