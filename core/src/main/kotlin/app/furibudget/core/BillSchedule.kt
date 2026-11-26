package app.furibudget.core

import java.time.LocalDate

/** When a bill falls due. Pure data so the rules can be tested without a database. */
data class BillSpec(
    val repeat: Repeat,
    val every: Int,
    val anchorDay: Int,
    val firstDue: LocalDate,
    val end: LocalDate? = null,
    /** Instalments: the series stops after this many payments. */
    val paymentsTotal: Int? = null,
)

/** One due date of a bill. [number] counts from 1 at the first due date. */
data class Occurrence(val due: LocalDate, val number: Int)

enum class PayState { PAID, SKIPPED }

enum class BillStatus { PAID, SKIPPED, OVERDUE, DUE_TODAY, DUE_SOON, UPCOMING }

object BillSchedule {
    /** How far back unpaid bills still count as overdue. Older ones drop out of lists and alerts. */
    const val LOOKBACK_DAYS = 90L

    private const val LIMIT = 6000

    /** Every due date in [from]..[to] inclusive, honouring the end date and the instalment count. */
    fun occurrences(spec: BillSpec, from: LocalDate, to: LocalDate): List<Occurrence> {
        val out = ArrayList<Occurrence>()
        var d = spec.firstDue
        var n = 1
        var guard = 0
        while (!d.isAfter(to) && guard++ < LIMIT) {
            if (spec.paymentsTotal != null && n > spec.paymentsTotal) break
            if (spec.end != null && d.isAfter(spec.end)) break
            if (!d.isBefore(from)) out += Occurrence(d, n)
            if (spec.repeat == Repeat.NONE) break
            d = Recurrence.next(d, spec.repeat, spec.anchorDay, spec.every)
            n++
        }
        return out
    }

    /** The due date after the last payment of a finite series, or null when it never ends. */
    fun lastDue(spec: BillSpec): LocalDate? {
        if (spec.repeat == Repeat.NONE) return spec.firstDue
        if (spec.paymentsTotal != null) {
            var d = spec.firstDue
            repeat(spec.paymentsTotal - 1) { d = Recurrence.next(d, spec.repeat, spec.anchorDay, spec.every) }
            return if (spec.end != null && d.isAfter(spec.end)) spec.end else d
        }
        return spec.end
    }

    /** Status of one due date. [remindDays] below zero means no "due soon" stage. */
    fun statusOf(due: LocalDate, state: PayState?, today: LocalDate, remindDays: Int): BillStatus = when {
        state == PayState.PAID -> BillStatus.PAID
        state == PayState.SKIPPED -> BillStatus.SKIPPED
        due.isBefore(today) -> BillStatus.OVERDUE
        due == today -> BillStatus.DUE_TODAY
        remindDays >= 0 && !due.isAfter(today.plusDays(remindDays.toLong())) -> BillStatus.DUE_SOON
        else -> BillStatus.UPCOMING
    }

    /** What the bill costs per month on average, for the subscription summary. Rounded to whole minor units. */
    fun monthlyCost(amountMinor: Long, repeat: Repeat, every: Int): Long {
        val n = every.coerceAtLeast(1)
        return when (repeat) {
            Repeat.NONE -> 0
            Repeat.DAILY -> Math.round(amountMinor * 365.0 / 12.0 / n)
            Repeat.WEEKLY -> Math.round(amountMinor * 52.0 / 12.0 / n)
            Repeat.MONTHLY -> Math.round(amountMinor.toDouble() / n)
            Repeat.YEARLY -> Math.round(amountMinor / 12.0 / n)
        }
    }
}
