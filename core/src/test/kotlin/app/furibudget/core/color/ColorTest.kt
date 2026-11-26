package app.furibudget.core.color

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test fun contrastExtremes() {
        assertEquals(21.0, contrastRatio(white, black), 0.01)
        assertEquals(1.0, contrastRatio(white, white), 0.001)
    }

    @Test fun roundTripStaysClose() {
        for (c in listOf(0xFF8B7CFF.toInt(), 0xFF22C55E.toInt(), 0xFFF97316.toInt(), 0xFF336699.toInt())) {
            val back = fromOklch(toOklch(c))
            for (shift in listOf(16, 8, 0)) {
                val d = abs(((c ushr shift) and 0xFF) - ((back ushr shift) and 0xFF))
                assertTrue(d <= 2, "channel diff $d for ${c.toString(16)}")
            }
        }
    }

    @Test fun ensureContrastReachesTarget() {
        val dark = 0xFF0B0B12.toInt()
        val weak = 0xFF2A2A66.toInt()
        assertTrue(contrastRatio(ensureContrast(weak, dark, 4.5), dark) >= 4.5)
        val light = 0xFFF6F5FB.toInt()
        val pale = 0xFFB9F0E0.toInt()
        assertTrue(contrastRatio(ensureContrast(pale, light, 4.5), light) >= 4.5)
    }

    @Test fun onColorPicksReadable() {
        assertEquals(white, onColor(0xFF1A1A2E.toInt()))
        assertEquals(black, onColor(0xFFFFE066.toInt()))
    }

    @Test fun derivedRolesAreReadableForAnyAccent() {
        val accents = listOf(0xFFFFFF00.toInt(), 0xFF000080.toInt(), 0xFF8B7CFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        for (dark in listOf(true, false)) {
            val surface = if (dark) 0xFF0B0B12.toInt() else 0xFFF6F5FB.toInt()
            for (a in accents) {
                val r = AccentDerivation.derive(a, dark, surface)
                assertTrue(contrastRatio(r.onAccent, r.accent) >= 4.5, "onAccent ${a.toString(16)} dark=$dark")
                assertTrue(contrastRatio(r.text, surface) >= 4.5, "text ${a.toString(16)} dark=$dark")
                assertTrue(contrastRatio(r.onContainer, r.container) >= 4.5, "container ${a.toString(16)} dark=$dark")
            }
        }
    }

    @Test fun chartSeriesDistinctHues() {
        val s = AccentDerivation.chartSeries(0xFF8B7CFF.toInt(), 6, true, 0xFF0B0B12.toInt())
        assertEquals(6, s.size)
        assertEquals(6, s.toSet().size)
        s.forEach { assertTrue(contrastRatio(it, 0xFF0B0B12.toInt()) >= 3.0) }
    }
}
