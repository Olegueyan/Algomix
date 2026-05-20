package fr.olegueyan.algomix.ui.theme

import fr.olegueyan.algomix.domain.settings.AppAppearance

data class AlgomixUiPalette(
    val background: Int,
    val surface: Int,
    val compactSurface: Int,
    val inputSurface: Int,
    val title: Int,
    val body: Int,
    val muted: Int,
    val accent: Int,
    val success: Int,
    val error: Int,
)

object AlgomixPalettes {
    fun from(appearance: AppAppearance): AlgomixUiPalette =
        when (appearance) {
            AppAppearance.LIGHT -> light
            AppAppearance.DARK -> dark
        }

    val light = AlgomixUiPalette(
        background = 0xFFF4F1EA.toInt(),
        surface = 0xFFFFFDF8.toInt(),
        compactSurface = 0xFFF7FBFF.toInt(),
        inputSurface = 0xFFF7FBFF.toInt(),
        title = 0xFF1A1A1A.toInt(),
        body = 0xFF212121.toInt(),
        muted = 0xFF4D5B75.toInt(),
        accent = 0xFFE65100.toInt(),
        success = 0xFF2E7D32.toInt(),
        error = 0xFFC62828.toInt(),
    )

    val dark = AlgomixUiPalette(
        background = 0xFF0D1117.toInt(),
        surface = 0xFF161B22.toInt(),
        compactSurface = 0xFF21262D.toInt(),
        inputSurface = 0xFF21262D.toInt(),
        title = 0xFFE6EDF3.toInt(),
        body = 0xFFE6EDF3.toInt(),
        muted = 0xFF8B949E.toInt(),
        accent = 0xFFFFAB40.toInt(),
        success = 0xFF2E7D32.toInt(),
        error = 0xFFC62828.toInt(),
    )
}
