package fr.olegueyan.algomix.ui.settings

import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.ui.components.rubik.RubikCubeAppearance

object CubeThemeAppearanceMapper {
    fun map(theme: CubeTheme): RubikCubeAppearance =
        when (theme) {
            CubeTheme.FILLED -> RubikCubeAppearance(
                backgroundColor = 0xFFF4F1EA.toInt(),
                bodyColor = 0xFF101114.toInt(),
            )
            CubeTheme.STICKER_ON_BLACK -> RubikCubeAppearance(
                backgroundColor = 0xFF12151D.toInt(),
                bodyColor = 0xFF050609.toInt(),
            )
            CubeTheme.CARBON -> RubikCubeAppearance(
                backgroundColor = 0xFFE8ECF0.toInt(),
                bodyColor = 0xFF23272F.toInt(),
            )
        }
}
