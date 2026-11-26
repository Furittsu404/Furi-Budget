package app.furibudget

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.data.AccountEntity
import app.furibudget.data.Ledger
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerTest {
    private val cash = AccountEntity(id = 1, name = "Cash", colorArgb = 0, icon = "cash", startBalance = 10_000)
    private val bank = AccountEntity(id = 2, name = "Bank", colorArgb = 0, icon = "bank")
    private val day = LocalDate.of(2026, 11, 10).toEpochDay()

    private fun tx(kind: TxKind, amt: Long, acc: Long, to: Long? = null, d: Long = day) =
        TransactionEntity(kind = kind, amountMinor = amt, accountId = acc, toAccountId = to, epochDay = d)

    @Test fun balancesFollowKinds() {
        val txs = listOf(
            tx(TxKind.INCOME, 50_000, 2),
            tx(TxKind.EXPENSE, 2_500, 1),
            tx(TxKind.TRANSFER, 20_000, 2, to = 1),
        )
        val b = Ledger.balances(listOf(cash, bank), txs)
        assertEquals(Money(10_000 - 2_500 + 20_000), b[1])
        assertEquals(Money(50_000 - 20_000), b[2])
    }

    @Test fun totalsIgnoreTransfersAndOutsideRange() {
        val nov = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        val txs = listOf(
            tx(TxKind.INCOME, 50_000, 2),
            tx(TxKind.EXPENSE, 2_500, 1),
            tx(TxKind.TRANSFER, 20_000, 2, to = 1),
            tx(TxKind.EXPENSE, 9_999, 1, d = LocalDate.of(2026, 10, 31).toEpochDay()),
        )
        val t = Ledger.totals(txs, nov)
        assertEquals(Money(50_000), t.income)
        assertEquals(Money(2_500), t.expense)
        assertEquals(Money(47_500), t.net)
    }
}
