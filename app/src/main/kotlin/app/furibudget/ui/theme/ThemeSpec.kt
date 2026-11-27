package app.furibudget.ui.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class BackgroundKind { MESH, AURORA, PARTICLES, GRADIENT, SOLID, PHOTO }

@Serializable
sealed interface AccentChoice {
    @Serializable @SerialName("swatch") data class Swatch(val id: String) : AccentChoice
    @Serializable @SerialName("dynamic") data object Dynamic : AccentChoice
    @Serializable @SerialName("custom") data class Custom(val argb: Int) : AccentChoice
    /** Picked from the background photo (M5); behaves like Custom until then. */
    @Serializable @SerialName("photo") data class Photo(val argb: Int) : AccentChoice
}

/** Everything the user can change about the look. The pack supplies the rest (see [ThemePack]). */
@Serializable
data class ThemeSpec(
    val packId: String = "glass",
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentChoice = AccentChoice.Swatch("iris"),
    val gradientAccent: Boolean = true,
    val colorblindSafe: Boolean = false,
    /** null follows the pack's own background. */
    val background: BackgroundKind? = null,
    /** 0 = still, 1 = full motion. Multiplies every animation the app owns. */
    val motion: Float = 1f,
    /** App-private copy of the chosen background photo; used when [background] is PHOTO. */
    val photoPath: String? = null,
    /** 0 to 1, scaled to 0 to 24 dp of blur. */
    val photoBlur: Float = 0.25f,
    /** 0 to 1 black overlay so text stays readable. */
    val photoDim: Float = 0.35f,
    /** Accent candidates pulled from the photo when it was chosen. */
    val photoColors: List<Int> = emptyList(),
    /** Average brightness of the photo, 0 to 1. Decides light or dark text over it. */
    val photoLuma: Float? = null,
    /** Card opacity for packs with see-through cards. Null keeps the pack's own look. */
    val cardAlpha: Float? = null,
    /** Shift the background a few dp as the phone tilts. */
    val parallax: Boolean = false,
    /** One accent colour and neutral ink: categories, accounts and amounts stop carrying their own colours. */
    val minimal: Boolean = false,
)
