package app.furibudget.data

import app.furibudget.core.BillSchedule
import app.furibudget.core.BillSpec
import app.furibudget.core.BillStatus
import app.furibudget.core.PayState
import java.time.LocalDate

/** One due date of a bill together with what happened to it. */
data class BillOccurrence(
    val bill: BillEntity,
    val due: LocalDate,
    val number: Int,
    val payment: BillPaymentEntity?,
    val status: BillStatus,
) {
    val isOpen: Boolean get() = status != BillStatus.PAID && status != BillStatus.SKIPPED
}

object Bills {
    fun spec(b: BillEntity) = BillSpec(
        repeat = b.repeat, every = b.repeatEvery, anchorDay = b.anchorDay,
        firstDue = LocalDate.ofEpochDay(b.firstDueEpochDay),
        end = b.endEpochDay?.let { LocalDate.ofEpochDay(it) }, paymentsTotal = b.paymentsTotal,
    )

    /** Every due date of the active bills in [from]..[to], with its status as of [today]. Sorted by date, then name. */
    fun occurrences(bills: List<BillEntity>, payments: List<BillPaymentEntity>, from: LocalDate, to: LocalDate, today: LocalDate): List<BillOccurrence> {
        val byKey = payments.associateBy { it.billId to it.dueEpochDay }
        val out = ArrayList<BillOccurrence>()
        for (b in bills) {
            if (!b.active) continue
            for (o in BillSchedule.occurrences(spec(b), from, to)) {
                val p = byKey[b.id to o.due.toEpochDay()]
                val state = p?.status?.let { if (it == PayStatus.PAID) PayState.PAID else PayState.SKIPPED }
                out += BillOccurrence(b, o.due, o.number, p, BillSchedule.statusOf(o.due, state, today, b.remindDaysBefore))
            }
        }
        return out.sortedWith(compareBy({ it.due }, { it.bill.name.lowercase() }))
    }

    /** Unpaid dates that already passed, within the look-back. */
    fun overdue(bills: List<BillEntity>, payments: List<BillPaymentEntity>, today: LocalDate): List<BillOccurrence> =
        occurrences(bills, payments, today.minusDays(BillSchedule.LOOKBACK_DAYS), today.minusDays(1), today).filter { it.status == BillStatus.OVERDUE }

    /** Open dates from today through [days] days ahead, plus anything overdue. */
    fun upcoming(bills: List<BillEntity>, payments: List<BillPaymentEntity>, today: LocalDate, days: Long): List<BillOccurrence> =
        occurrences(bills, payments, today.minusDays(BillSchedule.LOOKBACK_DAYS), today.plusDays(days), today).filter { it.isOpen }

    /** Payments made so far for an instalment series, counting skipped dates as done. */
    fun settledCount(billId: Long, payments: List<BillPaymentEntity>) = payments.count { it.billId == billId }
}
