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
