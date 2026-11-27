package app.furibudget.data

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.core.Periods
import java.time.LocalDate

/** Pure summaries over stored rows, shaped for the charts. */
object Insights {
    data class CategoryTotal(val categoryId: Long?, val amount: Money)

    /** Spending per category inside [range], biggest first. Uncategorised spending is grouped under null. */
    fun spendByCategory(txs: List<TransactionEntity>, range: DateRange, splits: List<TransactionSplitEntity> = emptyList()): List<CategoryTotal> =
        Spending.lines(txs, splits).filter { LocalDate.ofEpochDay(it.epochDay) in range }
            .groupBy { it.categoryId }
            .map { (id, list) -> CategoryTotal(id, Money(list.sumOf { it.amountMinor })) }
            .sortedByDescending { it.amount.minor }

    /** Spending per tag inside [range]. A transaction with several tags counts once under each of them. */
    fun spendByTag(txs: List<TransactionEntity>, range: DateRange, links: List<TransactionTagEntity>): List<Pair<Long, Money>> {
        val tagsOf = links.groupBy({ it.transactionId }, { it.tagId })
        return txs.filter { it.kind == TxKind.EXPENSE && it.role == TxRole.NORMAL && it.deletedAt == null && LocalDate.ofEpochDay(it.epochDay) in range }
            .flatMap { t -> tagsOf[t.id].orEmpty().map { it to t.amountMinor } }
            .groupBy({ it.first }, { it.second })
            .map { (id, v) -> id to Money(v.sum()) }
            .sortedByDescending { it.second.minor }
    }

    /** Cumulative spending for each day of [range]; days after [upTo] repeat the last value so the line stops flat. */
    fun cumulativeSpend(txs: List<TransactionEntity>, range: DateRange, upTo: LocalDate? = null): List<Long> {
        val perDay = LongArray(range.days.toInt())
        for (t in txs) {
            if (t.kind != TxKind.EXPENSE || t.role != TxRole.NORMAL) continue
            val d = LocalDate.ofEpochDay(t.epochDay)
            if (d in range) perDay[(t.epochDay - range.start.toEpochDay()).toInt()] += t.amountMinor
        }
        val last = if (upTo == null) perDay.lastIndex else (upTo.toEpochDay() - range.start.toEpochDay()).toInt().coerceIn(0, perDay.lastIndex)
        var run = 0L
        return perDay.mapIndexed { i, v -> if (i <= last) { run += v }; run }
    }

    data class MonthBar(val range: DateRange, val income: Money, val expense: Money)

    /** The [count] months ending with the one that holds [anchor], oldest first. */
    fun monthBars(txs: List<TransactionEntity>, anchor: LocalDate, count: Int, startDay: Int = 1): List<MonthBar> {
        val current = Periods.monthContaining(anchor, startDay)
        return (count - 1 downTo 0).map { back ->
            val r = Periods.shiftMonth(current, -back, startDay)
            val t = Ledger.totals(txs, r)
            MonthBar(r, t.income, t.expense)
        }
    }
}
