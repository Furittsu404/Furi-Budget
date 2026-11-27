package app.furibudget

import app.furibudget.data.LoanDirection
import app.furibudget.data.LoanEntity
import app.furibudget.data.People
import app.furibudget.data.PersonEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import app.furibudget.data.TxRole
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleTest {
    private val ana = PersonEntity(1, "Ana", colorArgb = 0)
    private val ben = PersonEntity(2, "Ben", colorArgb = 0)
    private val lent = LoanEntity(10, 1, LoanDirection.LENT, 1000, 100)
    private val owed = LoanEntity(11, 1, LoanDirection.OWED, 300, 100, dueEpochDay = 120)
    private fun repay(loan: Long, amt: Long, deleted: Long? = null) =
        TransactionEntity(kind = TxKind.INCOME, amountMinor = amt, accountId = 1, epochDay = 101, role = TxRole.REPAYMENT, loanId = loan, deletedAt = deleted)

    @Test fun outstandingIsPrincipalMinusRepayments() {
        val b = People.balances(listOf(ana), listOf(lent), listOf(repay(10, 250), repay(10, 250)))
        assertEquals(500L, b[0].loans[0].outstandingMinor)
        assertEquals(500L, b[0].netMinor)
        assertEquals(0.5f, b[0].loans[0].fraction, 0.001f)
    }

    @Test fun netCombinesBothDirections() {
        val b = People.balances(listOf(ana, ben), listOf(lent, owed), emptyList())
        assertEquals(700L, b[0].netMinor)
        assertEquals(0L, b[1].netMinor)
        assertEquals(1000L to 300L, People.totals(b))
    }

    @Test fun deletedRepaymentsDoNotCount() {
        val b = People.balances(listOf(ana), listOf(lent), listOf(repay(10, 400, deleted = 5L)))
        assertEquals(1000L, b[0].loans[0].outstandingMinor)
    }

    @Test fun settledAndOverdue() {
        val b = People.balances(listOf(ana), listOf(lent, owed), listOf(repay(10, 1000)))
        assertTrue(b[0].loans[0].settled)
        assertFalse(b[0].loans[1].settled)
        assertTrue(b[0].loans[1].overdue(LocalDate.ofEpochDay(130)))
        assertFalse(b[0].loans[1].overdue(LocalDate.ofEpochDay(110)))
    }

    @Test fun repaymentIsCappedToWhatIsOpen() {
        val st = People.balances(listOf(ana), listOf(lent), listOf(repay(10, 900)))[0].loans[0]
        assertEquals(100L, People.cap(5000, st))
        assertEquals(0L, People.cap(-5, st))
    }

    @Test fun loansAreNotSpendingOrIncome() {
        val loanTx = TransactionEntity(kind = TxKind.EXPENSE, amountMinor = 1000, accountId = 1, epochDay = 100, role = TxRole.LOAN, loanId = 10)
        val t = app.furibudget.data.Ledger.totals(listOf(loanTx, repay(10, 200)), app.furibudget.core.DateRange(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(200)))
        assertEquals(0L, t.expense.minor)
        assertEquals(0L, t.income.minor)
    }
}
