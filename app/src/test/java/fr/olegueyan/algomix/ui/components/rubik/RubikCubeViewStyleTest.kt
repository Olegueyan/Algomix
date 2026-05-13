package fr.olegueyan.algomix.ui.components.rubik

import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.rubik.scene.RubikSceneConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RubikCubeViewStyleTest {
    @Test
    fun usesDefaultsWhenNoAttributesAreProvided() {
        val style = RubikCubeViewStyle.from(RuntimeEnvironment.getApplication(), attrs = null)

        assertEquals(RubikCubeAppearance.DEFAULT_BACKGROUND_COLOR, style.appearance.backgroundColor)
        assertEquals(RubikCubeAppearance.DEFAULT_BODY_COLOR, style.appearance.bodyColor)
        assertTrue(style.doubleTapResetEnabled)
        assertEquals(RubikSceneConfiguration.MIN_ZOOM_FACTOR, style.zoomSettings.minZoomFactor, 0.0001f)
        assertEquals(RubikSceneConfiguration.INITIAL_ZOOM_FACTOR, style.zoomSettings.initialZoomFactor, 0.0001f)
        assertEquals(RubikSceneConfiguration.MAX_ZOOM_FACTOR, style.zoomSettings.maxZoomFactor, 0.0001f)
    }

    @Test
    fun readsAppearanceDoubleTapFlagAndZoomSettingsFromXmlAttributes() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.rubikBackgroundColor, "#FF112233")
            .addAttribute(R.attr.rubikBodyColor, "#FF445566")
            .addAttribute(R.attr.rubikEnableDoubleTapReset, "false")
            .addAttribute(R.attr.rubikMinZoomFactor, "0.85")
            .addAttribute(R.attr.rubikInitialZoomFactor, "1.15")
            .addAttribute(R.attr.rubikMaxZoomFactor, "1.8")
            .build()

        val style = RubikCubeViewStyle.from(RuntimeEnvironment.getApplication(), attrs)

        assertEquals(0xFF112233.toInt(), style.appearance.backgroundColor)
        assertEquals(0xFF445566.toInt(), style.appearance.bodyColor)
        assertFalse(style.doubleTapResetEnabled)
        assertEquals(0.85f, style.zoomSettings.minZoomFactor, 0.0001f)
        assertEquals(1.15f, style.zoomSettings.initialZoomFactor, 0.0001f)
        assertEquals(1.8f, style.zoomSettings.maxZoomFactor, 0.0001f)
    }
}
