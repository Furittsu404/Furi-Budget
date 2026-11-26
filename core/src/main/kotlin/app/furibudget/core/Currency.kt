package app.furibudget.core

import kotlin.math.abs

/** A currency and how many decimal places its minor unit has (PHP 2, JPY 0). */
data class Currency(val code: String, val symbol: String, val decimals: Int) {
    private val scale: Long = pow10(decimals)

    /** "18,420.50" without the symbol. The sign is "-" for negatives. */
    fun format(money: Money, withSymbol: Boolean = false): String {
        val v = abs(money.minor)
        val whole = (v / scale).toString().reversed().chunked(3).joinToString(",").reversed()
        val frac = if (decimals > 0) "." + (v % scale).toString().padStart(decimals, '0') else ""
        val sign = if (money.minor < 0) "-" else ""
        return sign + (if (withSymbol) "$symbol " else "") + whole + frac
    }

    /** Parses what a person types ("1,250.5", "1250", ".5"). Null when it is not a valid amount for this currency. */
    fun parse(text: String): Money? {
        val t = text.trim().replace(",", "").replace(" ", "")
        if (t.isEmpty()) return null
        val parts = t.split(".")
        if (parts.size > 2 || parts.any { p -> p.any { !it.isDigit() } }) return null
        val whole = parts[0].ifEmpty { "0" }
        val frac = parts.getOrNull(1).orEmpty()
        if (frac.length > decimals || whole.length > 12) return null
        return Money(whole.toLong() * scale + (frac.padEnd(decimals, '0').ifEmpty { "0" }).toLong())
    }

    companion object {
        private fun pow10(n: Int): Long {
            var r = 1L
            repeat(n) { r *= 10 }
            return r
        }

        val PHP = Currency("PHP", "₱", 2)
        val USD = Currency("USD", "$", 2)
        val EUR = Currency("EUR", "€", 2)
        val GBP = Currency("GBP", "£", 2)
        val JPY = Currency("JPY", "¥", 0)
        val all = listOf(PHP, USD, EUR, GBP, JPY, Currency("AUD", "A$", 2), Currency("CAD", "C$", 2), Currency("SGD", "S$", 2), Currency("INR", "₹", 2), Currency("KRW", "₩", 0))

        fun byCode(code: String) = all.firstOrNull { it.code == code } ?: PHP
    }
}
