package fr.olegueyan.algomix.application.rubik.interaction

/** Converts pinch span deltas into target zoom changes. */
internal class RubikPinchZoomController {
    private var lastSpan = 0f
    private var pinching = false

    fun onPointerDown(span: Float) {
        if (span > 0f) {
            lastSpan = span
            pinching = true
        }
    }

    fun onMove(
        span: Float,
        currentZoom: Float,
        minZoom: Float,
        maxZoom: Float,
        sensitivity: Float,
    ): Float {
        if (!pinching || span <= 0f || lastSpan <= 0f) {
            return currentZoom
        }

        val scaleFactor = span / lastSpan
        lastSpan = span
        val lowerBound = minZoom.coerceAtMost(maxZoom)
        val upperBound = maxZoom.coerceAtLeast(minZoom)
        val adjustedScaleFactor = 1f + (scaleFactor - 1f) * sensitivity.coerceAtLeast(0f)
        return (currentZoom / adjustedScaleFactor).coerceIn(lowerBound, upperBound)
    }

    fun onPointerUp() {
        reset()
    }

    fun onCancel() {
        reset()
    }

    fun isPinching(): Boolean = pinching

    private fun reset() {
        lastSpan = 0f
        pinching = false
    }
}
