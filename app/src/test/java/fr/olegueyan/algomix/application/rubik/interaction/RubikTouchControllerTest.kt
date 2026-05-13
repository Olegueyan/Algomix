package fr.olegueyan.algomix.application.rubik.interaction

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikTouchControllerTest {
    @Test
    fun movementBelowThresholdDoesNotTriggerDrag() {
        val drags = mutableListOf<Pair<Float, Float>>()
        val controller = RubikTouchController(10f) { dx, dy -> drags += dx to dy }

        controller.onDown(pointerId = 1, x = 0f, y = 0f)
        controller.onMove(pointerCount = 1, pointerId = 1, x = 3f, y = 4f)

        assertTrue(drags.isEmpty())
    }

    @Test
    fun rapidSeparateTapsDoNotTriggerDrag() {
        val drags = mutableListOf<Pair<Float, Float>>()
        val controller = RubikTouchController(10f) { dx, dy -> drags += dx to dy }

        controller.onDown(pointerId = 1, x = 0f, y = 0f)
        controller.onUp(pointerId = 1)
        controller.onDown(pointerId = 2, x = 100f, y = 100f)
        controller.onUp(pointerId = 2)

        assertTrue(drags.isEmpty())
    }

    @Test
    fun pointerTransitionResetsCurrentGesture() {
        val drags = mutableListOf<Pair<Float, Float>>()
        val controller = RubikTouchController(10f) { dx, dy -> drags += dx to dy }

        controller.onDown(pointerId = 1, x = 0f, y = 0f)
        controller.onPointerDown()
        controller.onPointerUp()
        controller.onMove(pointerCount = 1, pointerId = 1, x = 40f, y = 0f)

        assertTrue(drags.isEmpty())
    }

    @Test
    fun realDragTriggersRotationCallback() {
        val drags = mutableListOf<Pair<Float, Float>>()
        val controller = RubikTouchController(10f) { dx, dy -> drags += dx to dy }

        controller.onDown(pointerId = 1, x = 0f, y = 0f)
        controller.onMove(pointerCount = 1, pointerId = 1, x = 0f, y = 8f)
        controller.onMove(pointerCount = 1, pointerId = 1, x = 0f, y = 14f)
        controller.onMove(pointerCount = 1, pointerId = 1, x = 0f, y = 22f)

        assertFalse(drags.isEmpty())
    }
}
