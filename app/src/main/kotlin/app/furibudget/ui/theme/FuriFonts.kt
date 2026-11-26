package app.furibudget.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.furibudget.R

/** Bundled variable fonts (SIL OFL, see app/licenses). */
object FuriFonts {
    private val weights = listOf(400, 500, 600, 700, 800)

    private fun family(res: Int) = FontFamily(
        weights.map { w ->
            Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        },
    )

    val manrope: FontFamily by lazy { family(R.font.manrope) }
    val sora: FontFamily by lazy { family(R.font.sora) }

    // Ledger and Void use system faces for now (their chosen fonts could not be downloaded).
    val serif: FontFamily = FontFamily.Serif
    val sans: FontFamily = FontFamily.SansSerif
    val rounded: FontFamily by lazy { family(R.font.fredoka) }
}
