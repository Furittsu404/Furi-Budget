package app.furibudget.ui.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class BackgroundKind { MESH, GRADIENT, SOLID }

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
)
