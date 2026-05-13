package fr.olegueyan.algomix.ui.components.rubik

import org.junit.Assert.assertEquals
import org.junit.Test

class RubikCubeAppearanceTest {
    @Test
    fun defaultAppearanceMatchesViewDefaults() {
        val appearance = RubikCubeAppearance()

        assertEquals(RubikCubeAppearance.DEFAULT_BACKGROUND_COLOR, appearance.backgroundColor)
        assertEquals(RubikCubeAppearance.DEFAULT_BODY_COLOR, appearance.bodyColor)
    }
}
