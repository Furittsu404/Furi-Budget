package app.furibudget.core

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

object Recurrence {
    /** The occurrence after [from]. Monthly and yearly keep [anchorDay], shortened in short months (31st becomes 28th). */
    fun next(from: LocalDate, rule: Repeat, anchorDay: Int = from.dayOfMonth, every: Int = 1): LocalDate {
        val n = every.coerceAtLeast(1).toLong()
        return when (rule) {
            Repeat.NONE -> from
            Repeat.DAILY -> from.plusDays(n)
            Repeat.WEEKLY -> from.plusWeeks(n)
            Repeat.MONTHLY -> {
                val first = from.withDayOfMonth(1).plusMonths(n)
                first.withDayOfMonth(minOf(anchorDay, first.lengthOfMonth()))
            }
            Repeat.YEARLY -> {
                val first = from.withDayOfMonth(1).plusYears(n)
                first.withDayOfMonth(minOf(anchorDay, first.lengthOfMonth()))
            }
        }
    }

    /**
     * Every occurrence from [nextDue] through [through] (and not past [end]). Catches up on missed days after
     * the app was closed for a while. [limit] guards against a runaway loop on bad data.
     */
    fun dueDates(nextDue: LocalDate, rule: Repeat, anchorDay: Int, through: LocalDate, end: LocalDate? = null, limit: Int = 500, every: Int = 1): List<LocalDate> {
        if (rule == Repeat.NONE) return emptyList()
        val out = ArrayList<LocalDate>()
        var d = nextDue
        while (!d.isAfter(through) && (end == null || !d.isAfter(end)) && out.size < limit) {
            out += d
            d = next(d, rule, anchorDay, every)
        }
        return out
    }
}
