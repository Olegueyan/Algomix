package fr.olegueyan.algomix.application.rubik.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikSceneStateTest {
    @Test
    fun resetRotationRealignsOrientationButLeavesZoomUntouched() {
        val sceneState = RubikSceneState()

        val originalZoom = sceneState.camera.zoom
        sceneState.camera.zoom = sceneState.maxZoom
        sceneState.camera.drag(
            dx = 160f,
            dy = 90f,
            sensitivity = RubikSceneConfiguration.DRAG_SENSITIVITY,
        )

        sceneState.resetRotation()
        repeat(200) {
            sceneState.animateFrame()
        }

        assertTrue(
            sceneState.camera.isOrientationNearIso(
                threshold = RubikSceneConfiguration.RESET_ALIGNMENT_THRESHOLD,
                isoXDeg = RubikSceneConfiguration.ISO_X_DEG,
                isoYDeg = RubikSceneConfiguration.ISO_Y_DEG,
            )
        )
        assertEquals(sceneState.maxZoom, sceneState.camera.zoom, 0.0001f)
        assertFalse(sceneState.camera.zoom == originalZoom)
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
