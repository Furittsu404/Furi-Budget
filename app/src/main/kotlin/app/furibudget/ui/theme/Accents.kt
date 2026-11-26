package app.furibudget.ui.theme

data class AccentSwatch(val id: String, val name: String, val argb: Int)

object Accents {
    const val DEFAULT_ID = "iris"

    val swatches: List<AccentSwatch> = listOf(
        AccentSwatch("iris", "Iris", 0xFF8B7CFF.toInt()),
        AccentSwatch("cobalt", "Cobalt", 0xFF4F8DFF.toInt()),
        AccentSwatch("sky", "Sky", 0xFF38BDF8.toInt()),
        AccentSwatch("lagoon", "Lagoon", 0xFF2DD4BF.toInt()),
        AccentSwatch("mint", "Mint", 0xFF4ADE80.toInt()),
        AccentSwatch("lime", "Lime", 0xFFB8E62E.toInt()),
        AccentSwatch("sun", "Sun", 0xFFFACC15.toInt()),
        AccentSwatch("amber", "Amber", 0xFFFBA31E.toInt()),
        AccentSwatch("tangerine", "Tangerine", 0xFFFF7A3D.toInt()),
        AccentSwatch("coral", "Coral", 0xFFFF6B6B.toInt()),
        AccentSwatch("rose", "Rose", 0xFFFF5C93.toInt()),
        AccentSwatch("orchid", "Orchid", 0xFFE066FF.toInt()),
        AccentSwatch("grape", "Grape", 0xFFA855F7.toInt()),
        AccentSwatch("ice", "Ice", 0xFFA5F3FC.toInt()),
        AccentSwatch("sand", "Sand", 0xFFD6B98C.toInt()),
        AccentSwatch("slate", "Slate", 0xFF94A3B8.toInt()),
    )

    fun byId(id: String): AccentSwatch = swatches.firstOrNull { it.id == id } ?: swatches.first()
}
