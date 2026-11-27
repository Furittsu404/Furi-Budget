package app.furibudget.data

import app.furibudget.core.DateRange
import java.time.LocalDate

/** A sinking fund and what is reserved in it right now. */
data class FundStatus(val fund: FundEntity, val savedMinor: Long) {
    val fraction: Float get() = fund.targetMinor?.takeIf { it > 0 }?.let { (savedMinor.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
    val reached: Boolean get() = fund.targetMinor?.let { it > 0 && savedMinor >= it } == true
}

/** Where your money stands: what is in accounts, what is set aside, what bills still need, and what is truly free. */
data class FreeToSpend(val totalMinor: Long, val reservedMinor: Long, val billsDueMinor: Long) {
    val freeMinor: Long get() = totalMinor - reservedMinor - billsDueMinor
}

/** One account's slice of the picture: what it holds, what is set aside in it and what its own bills still need. */
data class AccountMoney(val account: AccountEntity, val balanceMinor: Long, val reservedMinor: Long, val billsDueMinor: Long) {
    val freeMinor: Long get() = balanceMinor - reservedMinor - billsDueMinor
}

object Funds {
    /** Per-account picture for [accounts]. A fund counts toward the account it is held in; bills toward the account they are paid from. */
    fun perAccount(
        accounts: List<AccountEntity>, balances: Map<Long, Long>, statuses: List<FundStatus>,
        bills: List<BillEntity>, payments: List<BillPaymentEntity>, today: LocalDate, monthEnd: LocalDate,
    ): List<AccountMoney> {
        val firstId = accounts.firstOrNull()?.id
        val reservedBy = statuses.groupBy { it.fund.accountId ?: firstId }.mapValues { (_, v) -> v.sumOf { it.savedMinor.coerceAtLeast(0) } }
        val dueBy = Bills.occurrences(bills, payments, today.minusDays(app.furibudget.core.BillSchedule.LOOKBACK_DAYS), monthEnd, today)
            .filter { it.isOpen && it.bill.kind == TxKind.EXPENSE }
            .groupBy { it.bill.accountId }.mapValues { (_, v) -> v.sumOf { it.bill.amountMinor } }
        return accounts.map { AccountMoney(it, balances[it.id] ?: 0L, reservedBy[it.id] ?: 0L, dueBy[it.id] ?: 0L) }
    }

    /** All open accounts together. Free to spend here is the sum of each account's own, so the two views always agree. */
    fun total(perAccount: List<AccountMoney>) =
        FreeToSpend(perAccount.sumOf { it.balanceMinor }, perAccount.sumOf { it.reservedMinor }, perAccount.sumOf { it.billsDueMinor })

    /** Reserved money stays inside accounts. Moves tied to a deleted transaction are ignored until it is restored. */
    fun statuses(funds: List<FundEntity>, moves: List<FundMoveEntity>, liveTxIds: Set<Long>): List<FundStatus> {
        val byFund = moves.filter { it.transactionId == null || it.transactionId in liveTxIds }.groupBy({ it.fundId }, { it.amountMinor }).mapValues { it.value.sum() }
        return funds.map { FundStatus(it, byFund[it.id] ?: 0L) }
    }

    fun reserved(statuses: List<FundStatus>) = statuses.sumOf { it.savedMinor.coerceAtLeast(0) }

    /** Expense bills not paid yet that fall due between today and the end of the budget month, plus anything overdue. */
    fun billsStillDue(bills: List<BillEntity>, payments: List<BillPaymentEntity>, today: LocalDate, monthEnd: LocalDate): Long =
        Bills.occurrences(bills, payments, today.minusDays(app.furibudget.core.BillSchedule.LOOKBACK_DAYS), monthEnd, today)
            .filter { it.isOpen && it.bill.kind == TxKind.EXPENSE }
            .sumOf { it.bill.amountMinor }

    fun freeToSpend(total: Long, statuses: List<FundStatus>, billsDue: Long) = FreeToSpend(total, reserved(statuses), billsDue)

    fun monthKey(day: LocalDate) = "%04d-%02d".format(day.year, day.monthValue)

    /** The monthly reserve moves still to post for [today]'s month. The unique (fund, month) index keeps this from doubling up. */
    fun monthlyToPost(funds: List<FundEntity>, today: LocalDate): List<FundMoveEntity> =
        funds.filter { (it.monthlyMinor ?: 0) > 0 }.map {
            FundMoveEntity(fundId = it.id, amountMinor = it.monthlyMinor!!, epochDay = today.toEpochDay(), note = "Monthly", monthKey = monthKey(today))
        }

    /** How much you may take out: never more than is in the fund. */
    fun capTakeOut(requested: Long, status: FundStatus) = requested.coerceIn(0, status.savedMinor.coerceAtLeast(0))
}
