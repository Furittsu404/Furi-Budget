package app.furibudget

import app.furibudget.data.AccountEntity
import app.furibudget.data.FundEntity
import app.furibudget.data.FundMoveEntity
import app.furibudget.data.Funds
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FundsTest {
    private val trip = FundEntity(1, "Trip", colorArgb = 0, targetMinor = 10_000, monthlyMinor = 2_000)
    private val gadget = FundEntity(2, "Gadget", colorArgb = 0)
    private fun move(fund: Long, amt: Long, tx: Long? = null) = FundMoveEntity(fundId = fund, amountMinor = amt, epochDay = 1, transactionId = tx)

    @Test fun savedIsTheSumOfMoves() {
        val s = Funds.statuses(listOf(trip, gadget), listOf(move(1, 5000), move(1, -1000), move(2, 300)), emptySet())
        assertEquals(4000L, s[0].savedMinor)
        assertEquals(0.4f, s[0].fraction, 0.001f)
        assertEquals(300L, s[1].savedMinor)
        assertEquals(0f, s[1].fraction, 0f)
    }

    @Test fun movesOfDeletedTransactionsAreIgnored() {
        val moves = listOf(move(1, 5000), move(1, -800, tx = 9))
        assertEquals(4200L, Funds.statuses(listOf(trip), moves, setOf(9L))[0].savedMinor)
        assertEquals(5000L, Funds.statuses(listOf(trip), moves, emptySet())[0].savedMinor)
    }

    @Test fun freeToSpendSubtractsReservedAndBills() {
        val s = Funds.statuses(listOf(trip, gadget), listOf(move(1, 4000), move(2, 1000)), emptySet())
        val f = Funds.freeToSpend(20_000, s, 3_000)
        assertEquals(5_000L, f.reservedMinor)
        assertEquals(12_000L, f.freeMinor)
        assertTrue(Funds.freeToSpend(1_000, s, 0).freeMinor < 0)
    }

    @Test fun monthlyPostsOncePerFundWithAMonthKey() {
        val posts = Funds.monthlyToPost(listOf(trip, gadget), LocalDate.of(2026, 11, 27))
        assertEquals(1, posts.size)
        assertEquals("2026-11", posts[0].monthKey)
        assertEquals(2_000L, posts[0].amountMinor)
    }

    @Test fun takeOutIsCappedToWhatIsSaved() {
        val st = Funds.statuses(listOf(trip), listOf(move(1, 700)), emptySet())[0]
        assertEquals(700L, Funds.capTakeOut(5_000, st))
        assertEquals(0L, Funds.capTakeOut(-1, st))
    }

    @Test fun reachedWhenTargetMet() {
        assertTrue(Funds.statuses(listOf(trip), listOf(move(1, 10_000)), emptySet())[0].reached)
    }

    @Test fun perAccountSplitsReservedAndBillsByAccount() {
        val cash = AccountEntity(1, "Cash", 0, "cash")
        val bank = AccountEntity(2, "Bank", 0, "bank")
        val inBank = trip.copy(accountId = 2)
        val inCash = gadget.copy(accountId = 1)
        val st = Funds.statuses(listOf(inBank, inCash), listOf(move(1, 4000), move(2, 300)), emptySet())
        val rent = app.furibudget.data.BillEntity(
            id = 1, name = "Rent", accountId = 2, amountMinor = 1500, repeat = app.furibudget.core.Repeat.MONTHLY, anchorDay = 28,
            firstDueEpochDay = LocalDate.of(2026, 11, 28).toEpochDay(), colorArgb = 0,
        )
        val rows = Funds.perAccount(listOf(cash, bank), mapOf(1L to 1_000L, 2L to 20_000L), st, listOf(rent), emptyList(), LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 30))
        assertEquals(300L, rows[0].reservedMinor)
        assertEquals(700L, rows[0].freeMinor)
        assertEquals(4000L, rows[1].reservedMinor)
        assertEquals(1500L, rows[1].billsDueMinor)
        assertEquals(14_500L, rows[1].freeMinor)
        val all = Funds.total(rows)
        assertEquals(21_000L, all.totalMinor)
        assertEquals(4300L, all.reservedMinor)
        assertEquals(rows.sumOf { it.freeMinor }, all.freeMinor)
    }
}
