package app.furibudget.data

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import java.time.LocalDate

/** Pure money maths over the stored rows. */
object Ledger {
    /** Current balance of every account: start balance plus everything recorded against it. */
    fun balances(accounts: List<AccountEntity>, txs: List<TransactionEntity>): Map<Long, Money> {
        val out = accounts.associate { it.id to Money(it.startBalance) }.toMutableMap()
        fun add(id: Long?, delta: Long) { if (id != null && id in out) out[id] = out.getValue(id) + Money(delta) }
        for (t in txs) when (t.kind) {
            TxKind.INCOME -> add(t.accountId, t.amountMinor)
            TxKind.EXPENSE -> add(t.accountId, -t.amountMinor)
            TxKind.TRANSFER -> { add(t.accountId, -t.amountMinor); add(t.toAccountId, t.amountMinor) }
        }
        return out
    }

    data class Totals(val income: Money, val expense: Money) {
        val net: Money get() = income - expense
    }

    /** Income and spending inside [range]. Transfers move money between accounts and count as neither. */
    fun totals(txs: List<TransactionEntity>, range: DateRange): Totals {
        var income = 0L
        var expense = 0L
        for (t in txs) {
            if (LocalDate.ofEpochDay(t.epochDay) !in range) continue
            when (t.kind) {
                TxKind.INCOME -> income += t.amountMinor
                TxKind.EXPENSE -> expense += t.amountMinor
                TxKind.TRANSFER -> Unit
            }
        }
        return Totals(Money(income), Money(expense))
    }
}
