package app.furibudget

import app.furibudget.data.RateFetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateFetcherTest {
    private val ok = """{"result":"success","provider":"x","base_code":"PHP","rates":{"PHP":1,"USD":0.0172,"JPY":2.6,"XXX":5.0,"EUR":0.0159}}"""

    @Test fun parsesKnownCurrenciesOnly() {
        val r = RateFetcher.parse(ok, "PHP")
        assertEquals(setOf("USD", "JPY", "EUR"), r.keys)
        assertFalse("PHP" in r)
        assertEquals(58_139_535L, r.getValue("USD"))
    }

    @Test fun rejectsFailuresAndJunk() {
        assertTrue(RateFetcher.parse("""{"result":"error","error-type":"unsupported-code"}""", "PHP").isEmpty())
        assertTrue(RateFetcher.parse("not json", "PHP").isEmpty())
        assertTrue(RateFetcher.parse("""{"result":"success","rates":{"USD":0}}""", "PHP").isEmpty())
    }
}
