package fr.olegueyan.algomix.application.rubik.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikPinchZoomControllerTest {
    @Test
    fun fingersMovingApartZoomIn() {
        val controller = RubikPinchZoomController()

        controller.onPointerDown(span = 100f)
        val zoom = controller.onMove(span = 120f, currentZoom = 8.6f, minZoom = 6.1f, maxZoom = 11.8f, sensitivity = 1f)

        assertTrue(zoom < 8.6f)
    }

    @Test
    fun fingersMovingCloserZoomOut() {
        val controller = RubikPinchZoomController()

        controller.onPointerDown(span = 100f)
        val zoom = controller.onMove(span = 80f, currentZoom = 8.6f, minZoom = 6.1f, maxZoom = 11.8f, sensitivity = 1f)

        assertTrue(zoom > 8.6f)
    }

    @Test
    fun pointerUpStopsPinch() {
        val controller = RubikPinchZoomController()

        controller.onPointerDown(span = 100f)
        controller.onPointerUp()

        assertFalse(controller.isPinching())
        assertEquals(
            8.6f,
            controller.onMove(span = 120f, currentZoom = 8.6f, minZoom = 6.1f, maxZoom = 11.8f, sensitivity = 1f),
            0.0001f,
        )
    }

    @Test
    fun customBoundsClampZoom() {
        val controller = RubikPinchZoomController()

        controller.onPointerDown(span = 100f)
        val zoomIn = controller.onMove(
            span = 180f,
            currentZoom = 8.6f,
            minZoom = 7.8f,
            maxZoom = 9.2f,
            sensitivity = 1f,
        )
        val zoomOut = controller.onMove(
            span = 20f,
            currentZoom = zoomIn,
            minZoom = 7.8f,
            maxZoom = 9.2f,
            sensitivity = 1f,
        )

        assertEquals(7.8f, zoomIn, 0.0001f)
        assertEquals(9.2f, zoomOut, 0.0001f)
    }

    @Test
    fun lowerSensitivitySoftensZoomChange() {
        val controller = RubikPinchZoomController()

        controller.onPointerDown(span = 100f)
        val softZoom = controller.onMove(
            span = 140f,
            currentZoom = 8.6f,
            minZoom = 6.1f,
            maxZoom = 11.8f,
            sensitivity = 0.5f,
        )

        controller.onPointerDown(span = 100f)
        val directZoom = controller.onMove(
            span = 140f,
            currentZoom = 8.6f,
            minZoom = 6.1f,
            maxZoom = 11.8f,
            sensitivity = 1f,
        )

        assertTrue(softZoom > directZoom)
    }
}
