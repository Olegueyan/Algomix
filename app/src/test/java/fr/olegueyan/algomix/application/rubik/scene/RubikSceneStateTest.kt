package fr.olegueyan.algomix.application.rubik.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikSceneStateTest {
    @Test
    fun resetViewRealignsOrientationAndReturnsToFitZoom() {
        val sceneState = RubikSceneState()

        sceneState.camera.zoom = sceneState.maxZoom
        sceneState.camera.drag(
            dx = 160f,
            dy = 90f,
            sensitivity = RubikSceneConfiguration.DRAG_SENSITIVITY,
        )

        sceneState.resetView()
        repeat(200) {
            sceneState.animateFrame()
        }

        assertEquals(sceneState.fitZoom, sceneState.camera.zoom, 0.0001f)
        assertTrue(
            sceneState.camera.isOrientationNearIso(
                threshold = RubikSceneConfiguration.RESET_ALIGNMENT_THRESHOLD,
                isoXDeg = RubikSceneConfiguration.ISO_X_DEG,
                isoYDeg = RubikSceneConfiguration.ISO_Y_DEG,
            )
        )
    }

    @Test
    fun resetViewReturnsToConfiguredInitialZoom() {
        val sceneState = RubikSceneState()
        sceneState.updateZoomSettings(
            RubikZoomSettings(
                minZoomFactor = 0.85f,
                initialZoomFactor = 1.15f,
                maxZoomFactor = 1.8f,
            )
        )

        sceneState.camera.zoom = sceneState.maxZoom
        sceneState.resetView()
        repeat(200) {
            sceneState.animateFrame()
        }

        assertEquals(sceneState.initialZoom, sceneState.camera.zoom, 0.0001f)
        assertTrue(sceneState.minZoom < sceneState.initialZoom)
        assertTrue(sceneState.maxZoom > sceneState.initialZoom)
    }

    @Test
    fun zoomMovesSmoothlyTowardsTargetZoom() {
        val sceneState = RubikSceneState()

        sceneState.camera.zoom = sceneState.maxZoom
        sceneState.camera.setTargetZoom(sceneState.fitZoom)

        sceneState.animateFrame()

        assertTrue(sceneState.camera.zoom < sceneState.maxZoom)
        assertTrue(sceneState.camera.zoom > sceneState.fitZoom)
    }

    @Test
    fun portraitViewportRequiresMoreDistanceThanSquareViewport() {
        val sceneState = RubikSceneState()
        val squareFitZoom = sceneState.fitZoom

        sceneState.updateViewport(width = 1080, height = 1920, verticalFovDeg = 45f)

        assertTrue(sceneState.fitZoom > squareFitZoom)
    }
}
