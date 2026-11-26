package app.furibudget.core

import java.time.LocalDate

/** Inclusive date range. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    val days: Long get() = end.toEpochDay() - start.toEpochDay() + 1
}

object Periods {
    /**
     * The budget month holding [date]. A month runs from [startDay] to the day before the next [startDay].
     * A start day past the month's length (e.g. 31 in April) counts as that month's last day.
     */
    fun monthContaining(date: LocalDate, startDay: Int = 1): DateRange {
        require(startDay in 1..31)
        val thisStart = startOf(date.year, date.monthValue, startDay)
        val start = if (date.isBefore(thisStart)) {
            val prev = date.withDayOfMonth(1).minusMonths(1)
            startOf(prev.year, prev.monthValue, startDay)
        } else thisStart
        val nextFirst = start.withDayOfMonth(1).plusMonths(1)
        val nextStart = startOf(nextFirst.year, nextFirst.monthValue, startDay)
        return DateRange(start, nextStart.minusDays(1))
    }

    fun shiftMonth(range: DateRange, months: Int, startDay: Int = 1): DateRange =
        monthContaining(range.start.plusMonths(months.toLong()).withDayOfMonth(1).let { startOf(it.year, it.monthValue, startDay) }, startDay)

    private fun startOf(year: Int, month: Int, startDay: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        return first.withDayOfMonth(minOf(startDay, first.lengthOfMonth()))
    }
}
