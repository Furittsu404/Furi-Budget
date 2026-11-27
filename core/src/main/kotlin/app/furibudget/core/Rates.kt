package app.furibudget.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exchange maths. A rate is kept as [rateMicro]: how many units of the main currency one unit of the foreign
 * currency is worth, times 1,000,000 (USD at 58.25 pesos is 58_250_000). Storing it this way keeps small-value
 * currencies such as KRW or JPY precise.
 */
object Rates {
    const val SCALE = 1_000_000L

    /** [foreignMinor] in the foreign currency's minor units, as minor units of [base]. */
    fun toBase(foreignMinor: Long, foreign: Currency, base: Currency, rateMicro: Long): Long =
        BigDecimal(foreignMinor).movePointLeft(foreign.decimals)
            .multiply(BigDecimal(rateMicro)).movePointLeft(6)
            .movePointRight(base.decimals)
            .setScale(0, RoundingMode.HALF_UP).toLong()

    /** From the online format (units of the foreign currency per one unit of the base) to [rateMicro]. Null for junk. */
    fun fromPerBase(foreignPerBase: Double): Long? {
        if (!foreignPerBase.isFinite() || foreignPerBase <= 0.0) return null
        val micro = BigDecimal.ONE.divide(BigDecimal(foreignPerBase), 12, RoundingMode.HALF_UP).movePointRight(6).setScale(0, RoundingMode.HALF_UP)
        return micro.toLong().takeIf { it > 0 }
    }

    /** What someone types ("58.25", "0.0172") as [rateMicro]. Null when it is not a positive number. */
    fun parseTyped(text: String): Long? {
        val d = text.trim().replace(",", "").toBigDecimalOrNull() ?: return null
        if (d.signum() <= 0) return null
        return d.movePointRight(6).setScale(0, RoundingMode.HALF_UP).toLong().takeIf { it > 0 }
    }

    /** A rate for reading: 2 decimals from 100 up, 4 from 1 up, 5 below 1; trailing zeros dropped. */
    fun display(rateMicro: Long): String {
        val d = BigDecimal(rateMicro).movePointLeft(6)
        val scale = when { rateMicro >= 100 * SCALE -> 2; rateMicro >= SCALE -> 4; else -> 5 }
        return d.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    /** [rateMicro] as plain text with up to six decimals and no trailing zeros. */
    fun format(rateMicro: Long): String = BigDecimal(rateMicro).movePointLeft(6).stripTrailingZeros().toPlainString()
}
