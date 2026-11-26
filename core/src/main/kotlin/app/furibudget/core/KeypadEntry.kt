package app.furibudget.core

/**
 * Amount typed on the on-screen keypad. Immutable: each key returns the next state.
 * Digits after the decimal point are capped at the currency's decimals; the whole part is capped
 * so the value always fits a Long of minor units.
 */
data class KeypadEntry(
    val whole: String = "",
    val frac: String = "",
    val hasPoint: Boolean = false,
    val decimals: Int = 2,
) {
    fun digit(d: Int): KeypadEntry {
        require(d in 0..9)
        return if (hasPoint) {
            if (frac.length >= decimals) this else copy(frac = frac + d)
        } else {
            val next = if (whole == "0") d.toString() else whole + d
            if (next.length > MAX_WHOLE) this else copy(whole = next)
        }
    }

    fun point(): KeypadEntry = if (decimals == 0 || hasPoint) this else copy(hasPoint = true, whole = whole.ifEmpty { "0" })

    fun backspace(): KeypadEntry = when {
        frac.isNotEmpty() -> copy(frac = frac.dropLast(1))
        hasPoint -> copy(hasPoint = false)
        else -> copy(whole = whole.dropLast(1))
    }

    val isEmpty: Boolean get() = whole.isEmpty() && !hasPoint

    fun toMoney(): Money {
        var scale = 1L
        repeat(decimals) { scale *= 10 }
        val w = (whole.ifEmpty { "0" }).toLong()
        val f = frac.padEnd(decimals, '0').ifEmpty { "0" }.toLong()
        return Money(w * scale + f)
    }

    /** What the amount display shows, keeping a trailing point or zeros the user typed. */
    fun display(): String {
        val w = (whole.ifEmpty { "0" }).reversed().chunked(3).joinToString(",").reversed()
        return if (hasPoint) "$w.$frac" else w
    }

    companion object {
        const val MAX_WHOLE = 10

        fun from(money: Money, decimals: Int): KeypadEntry {
            var scale = 1L
            repeat(decimals) { scale *= 10 }
            val whole = (money.minor / scale).toString()
            val frac = if (decimals > 0) (money.minor % scale).toString().padStart(decimals, '0').trimEnd('0') else ""
            return KeypadEntry(whole = if (money.minor == 0L) "" else whole, frac = frac, hasPoint = frac.isNotEmpty(), decimals = decimals)
        }
    }
}
