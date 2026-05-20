package fr.olegueyan.algomix.application.rubik.scene

import fr.olegueyan.algomix.domain.cube.Cubie
import fr.olegueyan.algomix.domain.cube.Move
import fr.olegueyan.algomix.domain.cube.MoveAxis
import fr.olegueyan.algomix.domain.cube.RubikCubeState
import fr.olegueyan.algomix.infrastructure.rendering.rubik.RubikCubeRenderSpec

internal class RubikSceneState(
    val cubeState: RubikCubeState = RubikCubeState(),
    val camera: RubikCameraState = RubikCameraState(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
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
    private var resetTargetQx = 0f
    private var resetTargetQy = 0f
    private var resetTargetQz = 0f
    private var resetTargetQw = 1f
    private var lastViewportWidth = 1
    private var lastViewportHeight = 1
    private var lastVerticalFovDeg = 45f

    private data class MoveAnimationStep(val move: Move, val finalState: RubikCubeState)

    private val moveQueue = ArrayDeque<MoveAnimationStep>()
    private var currentAnimatingMove: Move? = null
    private var moveProgress: Float = 0f
    private var moveStartTimeMs: Long = 0L
    private var pendingNextCubeState: RubikCubeState? = null

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

    /**
     * Animates the camera orientation back to the given target. Zoom is left untouched on purpose
     * — the user explicitly controls zoom and a double-tap should not undo it.
     */
    fun resetRotation(target: Quaternion = RubikResetTarget.isoQuaternion()) {
        resettingView = true
        resetTargetQx = target.x
        resetTargetQy = target.y
        resetTargetQz = target.z
        resetTargetQw = target.w
    }

    fun replaceCubeState(nextCubeState: RubikCubeState) {
        cubeState.cubies.clear()
        cubeState.cubies += nextCubeState.cubies
    }

    /**
     * Queues a move animation. The cube state is swapped to [finalState] when the animation
     * completes. Subsequent moves enqueue and play sequentially.
     */
    fun enqueueMoveAnimation(move: Move, finalState: RubikCubeState) {
        moveQueue.addLast(MoveAnimationStep(move, finalState))
        if (currentAnimatingMove == null) {
            startNextMoveAnimation()
        }
    }

    private fun startNextMoveAnimation() {
        val next = moveQueue.removeFirstOrNull() ?: return
        currentAnimatingMove = next.move
        pendingNextCubeState = next.finalState
        moveProgress = 0f
        moveStartTimeMs = nowProvider()
    }

    /**
     * Returns the in-flight rotation in degrees and the axis to rotate around for a cubie at the
     * given grid coordinates, or `null` if no move is animating or the cubie is not in the moving
     * layer.
     */
    fun currentMoveRotation(gx: Int, gy: Int, gz: Int): Pair<MoveAxis, Float>? {
        val move = currentAnimatingMove ?: return null
        if (!move.affectsCubie(gx, gy, gz)) return null
        val angle = move.effectiveQuarterTurns * QUARTER_TURN_DEGREES * moveProgress
        return move.axis to angle
    }

    fun currentMoveRotation(cubie: Cubie): Pair<MoveAxis, Float>? =
        currentMoveRotation(cubie.gx, cubie.gy, cubie.gz)

    fun animateFrame() {
        camera.animateZoom(RubikSceneConfiguration.ZOOM_SMOOTHING)
        advanceMoveAnimation()
        if (!resettingView) {
            return
        }
        camera.slerpOrientationTo(
            RubikSceneConfiguration.RESET_INTERPOLATION,
            resetTargetQx,
            resetTargetQy,
            resetTargetQz,
            resetTargetQw,
        )
        if (
            camera.isOrientationNear(
                threshold = RubikSceneConfiguration.RESET_ALIGNMENT_THRESHOLD,
                tx = resetTargetQx,
                ty = resetTargetQy,
                tz = resetTargetQz,
                tw = resetTargetQw,
            )
        ) {
            resettingView = false
        }
    }

    private fun advanceMoveAnimation() {
        if (currentAnimatingMove == null) return
        val elapsed = nowProvider() - moveStartTimeMs
        moveProgress = (elapsed.toFloat() / RubikSceneConfiguration.MOVE_ANIMATION_DURATION_MS).coerceAtMost(1f)
        if (moveProgress >= 1f) {
            pendingNextCubeState?.let { replaceCubeState(it) }
            currentAnimatingMove = null
            pendingNextCubeState = null
            moveProgress = 0f
            startNextMoveAnimation()
        }
    }

    val isAnimatingMove: Boolean
        get() = currentAnimatingMove != null

    private companion object {
        const val QUARTER_TURN_DEGREES = 90f
    }
}
