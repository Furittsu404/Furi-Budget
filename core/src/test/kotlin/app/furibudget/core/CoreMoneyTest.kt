package app.furibudget.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrencyTest {
    @Test fun formatsGroupingAndDecimals() {
        assertEquals("18,420.50", Currency.PHP.format(Money(1_842_050)))
        assertEquals("0.05", Currency.PHP.format(Money(5)))
        assertEquals("-1,000.00", Currency.PHP.format(Money(-100_000)))
        assertEquals("₱ 3.00", Currency.PHP.format(Money(300), withSymbol = true))
        assertEquals("1,234", Currency.JPY.format(Money(1234)))
    }
}

class ParseTest {
    @Test fun parsesTypedAmounts() {
        assertEquals(Money(125_050), Currency.PHP.parse("1,250.5"))
        assertEquals(Money(125_000), Currency.PHP.parse("1250"))
        assertEquals(Money(50), Currency.PHP.parse(".5"))
        assertEquals(Money(1234), Currency.JPY.parse("1234"))
    }

    @Test fun rejectsBadInput() {
        assertEquals(null, Currency.PHP.parse(""))
        assertEquals(null, Currency.PHP.parse("1.234"))
        assertEquals(null, Currency.PHP.parse("12a"))
        assertEquals(null, Currency.PHP.parse("1.2.3"))
        assertEquals(null, Currency.JPY.parse("1.5"))
    }
}

class KeypadEntryTest {
    private fun type(vararg keys: String, decimals: Int = 2): KeypadEntry =
        keys.fold(KeypadEntry(decimals = decimals)) { e, k ->
            when (k) { "." -> e.point(); "<" -> e.backspace(); else -> e.digit(k.toInt()) }
        }

    @Test fun buildsAmount() {
        assertEquals(Money(12_345), type("1", "2", "3", ".", "4", "5").toMoney())
        assertEquals(Money(1_230), type("1", "2", ".", "3").toMoney())
    }

    @Test fun capsDecimals() {
        assertEquals(Money(1_234), type("1", "2", ".", "3", "4", "5").toMoney())
        assertEquals(Money(55), type("5", ".", "5", decimals = 0).toMoney())
    }

    @Test fun leadingZeroAndPoint() {
        assertEquals("0.5", type(".", "5").display())
        assertEquals("7", type("0", "7").display())
    }

    @Test fun backspaceWalksBack() {
        val e = type("1", ".", "5", "<", "<", "<")
        assertTrue(e.isEmpty)
        assertEquals(Money(0), e.toMoney())
    }

    @Test fun capsLength() {
        val e = type(*Array(15) { "9" })
        assertEquals(KeypadEntry.MAX_WHOLE, e.whole.length)
    }

    @Test fun roundTripFromMoney() {
        val e = KeypadEntry.from(Money(1_250), 2)
        assertEquals(Money(1_250), e.toMoney())
        assertEquals("12.5", e.display())
    }
}

class PeriodsTest {
    @Test fun calendarMonth() {
        val r = Periods.monthContaining(LocalDate.of(2026, 11, 26))
        assertEquals(DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), r)
    }

    @Test fun customStartDayBeforeAndAfter() {
        val after = Periods.monthContaining(LocalDate.of(2026, 11, 26), 15)
        assertEquals(DateRange(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 14)), after)
        val before = Periods.monthContaining(LocalDate.of(2026, 11, 10), 15)
        assertEquals(DateRange(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 14)), before)
    }

    @Test fun startDayLongerThanMonth() {
        val r = Periods.monthContaining(LocalDate.of(2026, 2, 28), 31)
        assertEquals(LocalDate.of(2026, 2, 28), r.start)
        assertEquals(LocalDate.of(2026, 3, 30), r.end)
    }

    @Test fun yearBoundary() {
        val r = Periods.monthContaining(LocalDate.of(2027, 1, 3), 15)
        assertEquals(LocalDate.of(2026, 12, 15), r.start)
        assertEquals(LocalDate.of(2027, 1, 14), r.end)
    }

    @Test fun shiftMonth() {
        val nov = Periods.monthContaining(LocalDate.of(2026, 11, 26))
        assertEquals(LocalDate.of(2026, 10, 1), Periods.shiftMonth(nov, -1).start)
        assertEquals(LocalDate.of(2027, 1, 1), Periods.shiftMonth(nov, 2).start)
    }
}
