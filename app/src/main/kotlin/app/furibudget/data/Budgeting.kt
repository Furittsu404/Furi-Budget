package app.furibudget.data

import app.furibudget.core.DateRange
import app.furibudget.core.Money
import app.furibudget.core.Periods
import java.time.LocalDate

/** Budget and goal maths. Pure, so the rules live in tests rather than in the screens. */
object Budgeting {
    enum class Level { OK, CLOSE, OVER }

    data class Status(val limit: Money, val spent: Money) {
        val left: Money get() = limit - spent
        /** 0 for no limit; may exceed 1 when over. */
        val fraction: Float get() = if (limit.minor <= 0) 0f else spent.minor.toFloat() / limit.minor
        val level: Level get() = when {
            limit.minor > 0 && spent > limit -> Level.OVER
            fraction >= CLOSE_AT -> Level.CLOSE
            else -> Level.OK
        }
    }

    const val CLOSE_AT = 0.8f

    private fun spentIn(txs: List<TransactionEntity>, splits: List<TransactionSplitEntity>, range: DateRange, categoryId: Long?): Money {
        var sum = 0L
        for (l in Spending.lines(txs, splits)) {
            if (LocalDate.ofEpochDay(l.epochDay) !in range) continue
            if (categoryId == null || l.categoryId == categoryId) sum += l.amountMinor
        }
        return Money(sum)
    }

    /** The month or week of [b] that holds [today]. */
    fun rangeFor(b: BudgetEntity, today: LocalDate, startDay: Int, firstDay: java.time.DayOfWeek): DateRange =
        if (b.period == BudgetPeriod.WEEK) Periods.weekContaining(today, firstDay) else Periods.monthContaining(today, startDay)

    /** Limit for [range]: the base limit plus last period's unspent amount when rollover is on (never below zero). */
    fun status(b: BudgetEntity, txs: List<TransactionEntity>, range: DateRange, startDay: Int = 1, splits: List<TransactionSplitEntity> = emptyList()): Status {
        var limit = b.limitMinor
        if (b.rollover) {
            val prev = if (b.period == BudgetPeriod.WEEK) DateRange(range.start.minusWeeks(1), range.end.minusWeeks(1)) else Periods.shiftMonth(range, -1, startDay)
            limit += (b.limitMinor - spentIn(txs, splits, prev, b.categoryId).minor).coerceAtLeast(0)
        }
        return Status(Money(limit), spentIn(txs, splits, range, b.categoryId))
    }

    data class GoalPlan(val remaining: Money, val monthlyNeeded: Money?, val reached: Boolean, val overdue: Boolean)

    /** What a goal still needs; monthlyNeeded is spread over the whole months left until the deadline (at least one). */
    fun plan(g: GoalEntity, today: LocalDate): GoalPlan {
        val remaining = Money((g.targetMinor - g.savedMinor).coerceAtLeast(0))
        val reached = g.savedMinor >= g.targetMinor
        val deadline = g.deadlineEpochDay?.let { LocalDate.ofEpochDay(it) }
        if (reached || deadline == null) return GoalPlan(remaining, null, reached, false)
        if (!deadline.isAfter(today)) return GoalPlan(remaining, null, false, true)
        val months = (java.time.temporal.ChronoUnit.MONTHS.between(today.withDayOfMonth(1), deadline.withDayOfMonth(1)).toInt()).coerceAtLeast(1)
        return GoalPlan(remaining, Money((remaining.minor + months - 1) / months), false, false)
    }
}
