package app.furibudget.core

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {
    @Test fun addAndSubtract() {
        assertEquals(Money(150), Money(100) + Money(50))
        assertEquals(Money(-25), Money(25) - Money(50))
        assertEquals(Money(-5), -Money(5))
    }
}
