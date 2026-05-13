package fr.olegueyan.algomix.application.rubik.scene

import kotlin.math.atan
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

internal data class RubikZoomBounds(
    val minZoom: Float,
    val fitZoom: Float,
    val initialZoom: Float,
    val maxZoom: Float,
)

internal object RubikViewportFitCalculator {
    private const val MIN_ASPECT_RATIO = 0.1f

    /**
     * Computes the camera distance required to keep the full cube visible for the current
     * viewport. The cube is treated as a bounding sphere to stay safe for every orientation.
     */
    fun calculate(
        cubeRadius: Float,
        verticalFovDeg: Float,
        aspectRatio: Float,
        marginRatio: Float = RubikSceneConfiguration.FIT_MARGIN_RATIO,
        zoomSettings: RubikZoomSettings = RubikZoomSettings(),
    ): RubikZoomBounds {
        val normalizedZoomSettings = zoomSettings.normalized()
        val safeAspectRatio = aspectRatio.coerceAtLeast(MIN_ASPECT_RATIO)
        val paddedRadius = cubeRadius * (1f + marginRatio.coerceAtLeast(0f))
        val verticalHalfFovRad = Math.toRadians(verticalFovDeg.coerceIn(1f, 179f).toDouble() / 2.0)
        val horizontalHalfFovRad = atan(tan(verticalHalfFovRad) * safeAspectRatio.toDouble())
        val limitingHalfFovRad = min(verticalHalfFovRad, horizontalHalfFovRad)
        val fitZoom = (paddedRadius / sin(limitingHalfFovRad)).toFloat()

        return RubikZoomBounds(
            minZoom = fitZoom * normalizedZoomSettings.minZoomFactor,
            fitZoom = fitZoom,
            initialZoom = fitZoom * normalizedZoomSettings.initialZoomFactor,
            maxZoom = fitZoom * normalizedZoomSettings.maxZoomFactor,
        )
    }
}
