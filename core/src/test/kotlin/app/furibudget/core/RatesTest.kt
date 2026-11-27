package app.furibudget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatesTest {
    private val php = Currency.PHP
    private val usd = Currency.USD
    private val jpy = Currency.JPY

    @Test fun convertsUsdToPesos() {
        // $10.00 at 58.25 PHP per USD
        assertEquals(58_250L, Rates.toBase(1_000, usd, php, 58_250_000))
    }

    @Test fun convertsZeroDecimalCurrency() {
        // 1,000 yen at 0.39 PHP per yen = 390.00 PHP
        assertEquals(39_000L, Rates.toBase(1_000, jpy, php, 390_000))
    }

    @Test fun convertsIntoAZeroDecimalBase() {
        // $1.00 at 150 yen per USD with JPY as the main currency
        assertEquals(150L, Rates.toBase(100, usd, jpy, 150_000_000))
    }

    @Test fun roundsHalfUp() {
        assertEquals(2L, Rates.toBase(1, usd, php, 1_500_000)) // 0.01 USD * 1.5 = 0.015 PHP -> 2 centavos (1.5 rounds up)
    }

    @Test fun inversionFromOnlineFormatKeepsPrecision() {
        // The API says 0.01717 USD per PHP, so 1 USD is about 58.24 PHP
        val micro = Rates.fromPerBase(0.01717)!!
        assertEquals(58_241_118L, micro)
        assertNull(Rates.fromPerBase(0.0))
        assertNull(Rates.fromPerBase(Double.NaN))
        assertNull(Rates.fromPerBase(-3.0))
    }

    @Test fun typedRates() {
        assertEquals(58_250_000L, Rates.parseTyped("58.25"))
        assertEquals(17_200L, Rates.parseTyped("0.0172"))
        assertEquals(1_234_567_000L, Rates.parseTyped("1,234.567"))
        assertNull(Rates.parseTyped("abc"))
        assertNull(Rates.parseTyped("0"))
        assertNull(Rates.parseTyped("-1"))
    }

    @Test fun displayRoundsForReading() {
        assertEquals("63.7796", Rates.display(63_779_578))
        assertEquals("0.04749", Rates.display(47_492))
        assertEquals("0.4001", Rates.display(400_103))
        assertEquals("1234.57", Rates.display(1_234_567_000))
    }

    @Test fun formatsWithoutTrailingZeros() {
        assertEquals("58.25", Rates.format(58_250_000))
        assertEquals("0.0172", Rates.format(17_200))
        assertEquals("150", Rates.format(150_000_000))
    }
}
