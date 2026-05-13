package fr.olegueyan.algomix.application.rubik.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikCameraStateTest {
    @Test
    fun setTargetZoomClampsBoundsWithoutChangingCurrentZoom() {
        val cameraState = RubikCameraState()

        cameraState.updateZoomBounds(minZoom = 6.1f, maxZoom = 11.8f)
        cameraState.zoom = 8.6f
        cameraState.setTargetZoom(zoom = 4f)

        assertEquals(8.6f, cameraState.zoom, 0.0001f)
        assertEquals(6.1f, cameraState.targetZoom, 0.0001f)
    }

    @Test
    fun animateZoomMovesCurrentZoomTowardsTargetZoom() {
        val cameraState = RubikCameraState()

        cameraState.updateZoomBounds(minZoom = 6.1f, maxZoom = 11.8f)
        cameraState.zoom = 8.6f
        cameraState.setTargetZoom(zoom = 11.8f)
        cameraState.animateZoom(amount = 0.25f)

        assertTrue(cameraState.zoom > 8.6f)
        assertTrue(cameraState.zoom < 11.8f)
    }

    @Test
    fun updatingBoundsCanResetToFitZoom() {
        val cameraState = RubikCameraState()

        cameraState.zoom = 9.2f
        cameraState.updateZoomBounds(minZoom = 4.6f, maxZoom = 6.7f, resetZoom = true)

        assertEquals(4.6f, cameraState.zoom, 0.0001f)
        assertEquals(4.6f, cameraState.targetZoom, 0.0001f)
    }
}
