package fr.olegueyan.algomix.application.rubik.scene

import fr.olegueyan.algomix.domain.cube.RubikCubeState
import fr.olegueyan.algomix.infrastructure.rendering.rubik.RubikCubeRenderSpec
import kotlin.math.abs

internal class RubikSceneState(
    val cubeState: RubikCubeState = RubikCubeState(),
    val camera: RubikCameraState = RubikCameraState(),
) {
    private var zoomSettings = RubikZoomSettings()

    var minZoom = 1f
        private set

    var fitZoom = 1f
        private set

    var initialZoom = 1f
        private set

    var maxZoom = RubikSceneConfiguration.MAX_ZOOM_FACTOR
        private set

    private var viewportInitialized = false
    private var resettingView = false
    private var lastViewportWidth = 1
    private var lastViewportHeight = 1
    private var lastVerticalFovDeg = 45f

    init {
        applyViewport(width = 1, height = 1, verticalFovDeg = 45f, markViewportInitialized = false)
    }

    fun updateViewport(width: Int, height: Int, verticalFovDeg: Float) {
        applyViewport(width, height, verticalFovDeg, markViewportInitialized = true)
    }

    fun updateZoomSettings(settings: RubikZoomSettings) {
        zoomSettings = settings.normalized()
        applyViewport(
            width = lastViewportWidth,
            height = lastViewportHeight,
            verticalFovDeg = lastVerticalFovDeg,
            markViewportInitialized = false,
        )
    }

    private fun applyViewport(
        width: Int,
        height: Int,
        verticalFovDeg: Float,
        markViewportInitialized: Boolean,
    ) {
        lastViewportWidth = width
        lastViewportHeight = height
        lastVerticalFovDeg = verticalFovDeg

        val bounds = RubikViewportFitCalculator.calculate(
            cubeRadius = RubikCubeRenderSpec.boundingRadius,
            verticalFovDeg = verticalFovDeg,
            aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat(),
            zoomSettings = zoomSettings,
        )

        minZoom = bounds.minZoom
        fitZoom = bounds.fitZoom
        initialZoom = bounds.initialZoom
        maxZoom = bounds.maxZoom

        if (!viewportInitialized) {
            camera.updateZoomBounds(minZoom, maxZoom, resetZoom = true)
            camera.resetToIso(zoom = initialZoom)
            viewportInitialized = markViewportInitialized
            return
        }

        camera.updateZoomBounds(minZoom, maxZoom)
    }

    fun resetView() {
        resettingView = true
        camera.setTargetZoom(initialZoom)
    }

    fun animateFrame() {
        camera.animateZoom(RubikSceneConfiguration.ZOOM_SMOOTHING)
        if (!resettingView) {
            return
        }

        camera.setTargetZoom(initialZoom)
        camera.slerpOrientationToIso(
            amount = RubikSceneConfiguration.RESET_INTERPOLATION,
            isoXDeg = RubikSceneConfiguration.ISO_X_DEG,
            isoYDeg = RubikSceneConfiguration.ISO_Y_DEG,
        )
        if (
            camera.isOrientationNearIso(
                threshold = RubikSceneConfiguration.RESET_ALIGNMENT_THRESHOLD,
                isoXDeg = RubikSceneConfiguration.ISO_X_DEG,
                isoYDeg = RubikSceneConfiguration.ISO_Y_DEG,
            ) && abs(camera.zoom - initialZoom) < 0.001f
        ) {
            resettingView = false
        }
    }
}
