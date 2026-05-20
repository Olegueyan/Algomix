package fr.olegueyan.algomix.ui.settings

import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.ui.components.rubik.RubikCubeAppearance

object CubeThemeAppearanceMapper {
    fun map(theme: CubeTheme): RubikCubeAppearance =
        map(theme, RubikCubeAppearance.DEFAULT_BACKGROUND_COLOR)

    fun map(theme: CubeTheme, backgroundColor: Int): RubikCubeAppearance =
        when (theme) {
            CubeTheme.FILLED -> RubikCubeAppearance(
                backgroundColor = backgroundColor,
                bodyColor = 0xFF101114.toInt(),
            )
            CubeTheme.STICKER_ON_BLACK -> RubikCubeAppearance(
                backgroundColor = backgroundColor,
                bodyColor = 0xFF050609.toInt(),
            )
            CubeTheme.CARBON -> RubikCubeAppearance(
                backgroundColor = backgroundColor,
                bodyColor = 0xFF23272F.toInt(),
            )
        }
}
