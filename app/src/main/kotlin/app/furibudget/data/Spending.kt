package app.furibudget.data

import java.time.LocalDate

/** One amount attributed to one category on one day. A split transaction yields one line per slice. */
data class SpendLine(val categoryId: Long?, val amountMinor: Long, val epochDay: Long, val txId: Long)

object Spending {
    /**
     * Lines for transactions of [kind] that count as real spending or income (role NORMAL, not deleted).
     * A transaction with split slices contributes its slices instead of its own category.
     */
    fun lines(txs: List<TransactionEntity>, splits: List<TransactionSplitEntity>, kind: TxKind = TxKind.EXPENSE): List<SpendLine> {
        val byTx = splits.groupBy { it.transactionId }
        val out = ArrayList<SpendLine>()
        for (t in txs) {
            if (t.kind != kind || t.role != TxRole.NORMAL || t.deletedAt != null) continue
            val slices = byTx[t.id]
            if (slices.isNullOrEmpty()) out += SpendLine(t.categoryId, t.amountMinor, t.epochDay, t.id)
            else slices.forEach { out += SpendLine(it.categoryId, it.amountMinor, t.epochDay, t.id) }
        }
        return out
    }

    /** True when the slices add up to the transaction amount exactly. */
    fun balanced(amountMinor: Long, slices: List<TransactionSplitEntity>) = slices.isNotEmpty() && slices.sumOf { it.amountMinor } == amountMinor

    fun inRange(lines: List<SpendLine>, from: LocalDate, to: LocalDate) =
        lines.filter { LocalDate.ofEpochDay(it.epochDay).let { d -> !d.isBefore(from) && !d.isAfter(to) } }
}
