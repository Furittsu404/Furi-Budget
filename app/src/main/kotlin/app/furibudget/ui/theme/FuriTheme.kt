package app.furibudget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF7C5CFF)

private val Dark = darkColorScheme(primary = Accent, background = Color(0xFF0B0B12), surface = Color(0xFF14141F))
private val Light = lightColorScheme(primary = Accent, background = Color(0xFFF6F5FB), surface = Color.White)

/** M0 stub. M1 replaces this with the ThemeSpec system (see the plan file). */
@Composable
fun FuriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
