package fr.olegueyan.algomix.application.rubik.interaction

import kotlin.math.hypot

/** Tracks single-pointer drags and ignores accidental taps or interrupted gestures. */
internal class RubikTouchController(
    dragThresholdPx: Float,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
) {
    var dragThresholdPx = dragThresholdPx
        set(value) {
            field = value.coerceAtLeast(0f)
        }

    var activePointerId: Int? = null
        private set

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var awaitingFreshDown = false

    fun onDown(pointerId: Int, x: Float, y: Float) {
        activePointerId = pointerId
        downX = x
        downY = y
        lastX = x
        lastY = y
        isDragging = false
        awaitingFreshDown = false
    }

    fun onMove(pointerCount: Int, pointerId: Int, x: Float, y: Float) {
        if (awaitingFreshDown || pointerCount != 1 || activePointerId != pointerId) {
            return
        }

        if (!isDragging) {
            val totalDx = x - downX
            val totalDy = y - downY
            if (hypot(totalDx.toDouble(), totalDy.toDouble()) < dragThresholdPx.toDouble()) {
                lastX = x
                lastY = y
                return
            }
            isDragging = true
        }

        val dx = x - lastX
        val dy = y - lastY
        if (dx != 0f || dy != 0f) {
            onDrag(dx, dy)
        }
        lastX = x
        lastY = y
    }

    fun onPointerDown() {
        interruptGesture()
    }

    fun onPointerUp() {
        interruptGesture()
    }

    fun onUp(pointerId: Int) {
        if (activePointerId == pointerId) {
            clearGesture()
        }
    }

    fun onCancel() {
        interruptGesture()
    }

    private fun interruptGesture() {
        activePointerId = null
        isDragging = false
        awaitingFreshDown = true
    }

    private fun clearGesture() {
        activePointerId = null
        isDragging = false
    }
}
