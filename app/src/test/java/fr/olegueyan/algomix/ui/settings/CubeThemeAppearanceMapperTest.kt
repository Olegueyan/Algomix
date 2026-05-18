package fr.olegueyan.algomix.ui.settings

import fr.olegueyan.algomix.domain.settings.CubeTheme
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CubeThemeAppearanceMapperTest {
    @Test
    fun mapsCubeThemesToDistinctAppearances() {
        val filled = CubeThemeAppearanceMapper.map(CubeTheme.FILLED)
        val sticker = CubeThemeAppearanceMapper.map(CubeTheme.STICKER_ON_BLACK)
        val carbon = CubeThemeAppearanceMapper.map(CubeTheme.CARBON)

        assertNotEquals(filled, sticker)
        assertNotEquals(sticker, carbon)
        assertNotEquals(filled, carbon)
    }
}
