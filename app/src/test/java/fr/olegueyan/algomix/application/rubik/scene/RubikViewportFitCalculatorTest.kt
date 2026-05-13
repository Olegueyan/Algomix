package fr.olegueyan.algomix.application.rubik.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikViewportFitCalculatorTest {
    @Test
    fun computesFitZoomForSquareViewport() {
        val bounds = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
        )

        assertTrue(bounds.fitZoom > 0f)
        assertTrue(bounds.maxZoom > bounds.fitZoom)
    }

    @Test
    fun landscapeViewportDoesNotNeedMoreDistanceThanSquareViewport() {
        val square = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
        )
        val landscape = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1.8f,
        )

        assertTrue(landscape.fitZoom <= square.fitZoom)
    }

    @Test
    fun portraitViewportNeedsMoreDistanceThanSquareViewport() {
        val square = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
        )
        val portrait = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 9f / 16f,
        )

        assertTrue(portrait.fitZoom > square.fitZoom)
    }

    @Test
    fun marginIncreasesRequiredFitZoom() {
        val withoutMargin = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
            marginRatio = 0f,
        )
        val withMargin = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
            marginRatio = 0.12f,
        )

        assertTrue(withMargin.fitZoom > withoutMargin.fitZoom)
    }

    @Test
    fun maxZoomUsesConfiguredMultiplier() {
        val bounds = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
            zoomSettings = RubikZoomSettings(maxZoomFactor = 1.45f),
        )

        assertEquals(bounds.fitZoom * 1.45f, bounds.maxZoom, 0.0001f)
    }

    @Test
    fun zoomBoundsUseConfiguredMultipliers() {
        val bounds = RubikViewportFitCalculator.calculate(
            cubeRadius = 1.75f,
            verticalFovDeg = 45f,
            aspectRatio = 1f,
            zoomSettings = RubikZoomSettings(
                minZoomFactor = 0.85f,
                initialZoomFactor = 1.15f,
                maxZoomFactor = 1.8f,
            ),
        )

        assertEquals(bounds.fitZoom * 0.85f, bounds.minZoom, 0.0001f)
        assertEquals(bounds.fitZoom * 1.15f, bounds.initialZoom, 0.0001f)
        assertEquals(bounds.fitZoom * 1.8f, bounds.maxZoom, 0.0001f)
    }
}
