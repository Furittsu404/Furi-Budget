package app.furibudget

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
}
