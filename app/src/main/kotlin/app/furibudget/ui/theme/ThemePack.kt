package app.furibudget.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CardStyle { GLASS, OUTLINE }

/** Spring feel for one pack. Scaled by the user's motion setting. */
data class MotionProfile(val dampingRatio: Float, val stiffness: Float)

/** Fixed colours of one pack in one brightness. The accent is layered on top by [resolveColors]. */
data class PackPalette(
    val background: Color,
    val card: Color,
    val cardBorder: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    /** Two blob colours for the mesh background; the third blob always follows the accent. */
    val mesh: List<Color>,
    /** How strongly the accent-coloured third blob shows. */
    val accentBlob: Float,
)

data class ThemePack(
    val id: String,
    val name: String,
    val tagline: String,
    /** null means the pack has no palette for that brightness and is locked to the other. */
    val dark: PackPalette?,
    val light: PackPalette?,
    val cardStyle: CardStyle,
    val corner: Dp,
    val motion: MotionProfile,
    val defaultBackground: BackgroundKind,
    val family: () -> FontFamily,
)

object ThemePacks {
    val glass = ThemePack(
        id = "glass",
        name = "Tidepool",
        tagline = "Frosted glass over slow-drifting colour",
        dark = PackPalette(
            background = Color(0xFF07121C),
            card = Color(0x14FFFFFF),
            cardBorder = Color(0x38FFFFFF),
            onSurface = Color(0xFFEAF2F7),
            onSurfaceVariant = Color(0xFF9FB3C2),
            mesh = listOf(Color(0x8C0FA3A3), Color(0x804B4DED)),
            accentBlob = 0.50f,
        ),
        light = PackPalette(
            background = Color(0xFFEAF3F7),
            card = Color(0xA6FFFFFF),
            cardBorder = Color(0xCCFFFFFF),
            onSurface = Color(0xFF0E1E2A),
            onSurfaceVariant = Color(0xFF50657A),
            mesh = listOf(Color(0x997FE0D8), Color(0x99A9B4FF)),
            accentBlob = 0.55f,
        ),
        cardStyle = CardStyle.GLASS,
        corner = 28.dp,
        motion = MotionProfile(dampingRatio = 0.8f, stiffness = 220f),
        defaultBackground = BackgroundKind.MESH,
        family = { FuriFonts.manrope },
    )

    val neon = ThemePack(
        id = "neon",
        name = "Afterhours",
        tagline = "Near-black, sharp outlines, light where it counts",
        dark = PackPalette(
            background = Color(0xFF05050A),
            card = Color(0xFF0E0E18),
            cardBorder = Color(0xFF23233A),
            onSurface = Color(0xFFF2F2FA),
            onSurfaceVariant = Color(0xFF9A9AB8),
            mesh = listOf(Color(0x801B1B5E), Color(0x733A0E5C)),
            accentBlob = 0.14f,
        ),
        light = null,
        cardStyle = CardStyle.OUTLINE,
        corner = 14.dp,
        motion = MotionProfile(dampingRatio = 0.9f, stiffness = 500f),
        defaultBackground = BackgroundKind.MESH,
        family = { FuriFonts.sora },
    )

    val all = listOf(glass, neon)

    fun byId(id: String): ThemePack = all.firstOrNull { it.id == id } ?: glass
}
