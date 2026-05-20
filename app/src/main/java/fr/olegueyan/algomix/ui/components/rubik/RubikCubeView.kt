package fr.olegueyan.algomix.ui.components.rubik

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import fr.olegueyan.algomix.application.rubik.interaction.RubikPinchZoomController
import fr.olegueyan.algomix.application.rubik.interaction.RubikTouchController
import fr.olegueyan.algomix.application.rubik.scene.Quaternion
import fr.olegueyan.algomix.application.rubik.scene.RubikResetTarget
import fr.olegueyan.algomix.application.rubik.scene.RubikSceneConfiguration
import fr.olegueyan.algomix.application.rubik.scene.RubikSceneState
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.Move
import fr.olegueyan.algomix.domain.cube.RubikCubeState
import fr.olegueyan.algomix.infrastructure.rendering.rubik.RubikCubeRenderStateMapper
import fr.olegueyan.algomix.infrastructure.rendering.rubik.RubikRenderer
import kotlin.math.sqrt

/**
 * Interactive Rubik's cube OpenGL view with a deliberately small public API.
 *
 * Camera tuning and rendering geometry remain internal so the view can stay predictable and the
 * cube always fits inside its bounds.
 */
class RubikCubeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val sceneState = RubikSceneState()
    private val cubeRenderer = RubikRenderer(sceneState)
    private var rendererAttached = false
    private var doubleTapResetEnabled = true
    private var rotationLocked = false
    private val touchController = RubikTouchController(
        dragThresholdPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
    ) { dx, dy ->
        queueEvent {
            sceneState.camera.drag(dx, dy, RubikSceneConfiguration.DRAG_SENSITIVITY)
        }
    }
    private val pinchZoomController = RubikPinchZoomController()
    private var consumingDoubleTap = false

    /** Invoked on the UI thread when a double tap occurs and reset is enabled. */
    var onDoubleTapResetListener: (() -> Unit)? = null

    val cubeState: RubikCubeState
        get() = sceneState.cubeState

    var appearance: RubikCubeAppearance
        get() = cubeRenderer.appearance
        set(value) {
            cubeRenderer.appearance = value
            if (rendererAttached) {
                requestRender()
            }
        }

    private val doubleTapDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(event: MotionEvent): Boolean {
                    consumingDoubleTap = true
                    cancelActiveInteraction()
                    if (!doubleTapResetEnabled) {
                        return false
                    }
                    val listener = onDoubleTapResetListener
                    if (listener != null) {
                        listener()
                    } else {
                        resetRotation()
                    }
                    return true
                }
            },
        )

    init {
        applyViewStyle(RubikCubeViewStyle.from(context, attrs))
        setEGLContextClientVersion(2)
        setRenderer(cubeRenderer)
        rendererAttached = true
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handledDoubleTap = doubleTapDetector.onTouchEvent(event)
        if (handledDoubleTap || consumingDoubleTap) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                consumingDoubleTap = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            cancelActiveInteraction()
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchController.onDown(event.getPointerId(0), event.x, event.y)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchController.onPointerDown()
                pinchZoomController.onPointerDown(currentPinchSpan(event))
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && pinchZoomController.isPinching()) {
                    handlePinchMove(event)
                    return true
                }

                if (event.pointerCount != 1) {
                    return true
                }

                if (rotationLocked) {
                    return true
                }

                handleSinglePointerMove(event)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                touchController.onPointerUp()
                pinchZoomController.onPointerUp()
            }

            MotionEvent.ACTION_UP -> {
                touchController.onUp(event.getPointerId(event.actionIndex))
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_CANCEL -> {
                touchController.onCancel()
                pinchZoomController.onCancel()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return true
    }

    /** Animates the camera back to the given orientation. Zoom is preserved. */
    fun resetRotation(target: Quaternion = RubikResetTarget.isoQuaternion()) {
        if (rendererAttached) {
            queueEvent {
                sceneState.resetRotation(target)
            }
            requestRender()
            return
        }
        sceneState.resetRotation(target)
    }

    /**
     * Replaces the cube state shown by the view. No-op while a move animation is in flight — the
     * animation drives the final state once it completes.
     */
    fun renderCube(cubeState: CubeState) {
        val renderState = RubikCubeRenderStateMapper.map(cubeState)
        if (rendererAttached) {
            queueEvent {
                if (!sceneState.isAnimatingMove) {
                    sceneState.replaceCubeState(renderState)
                }
            }
            requestRender()
            return
        }
        sceneState.replaceCubeState(renderState)
    }

    /** Plays a per-move animation and swaps to [finalState] once it completes. */
    fun playMove(move: Move, finalState: CubeState) {
        val renderState = RubikCubeRenderStateMapper.map(finalState)
        if (rendererAttached) {
            queueEvent {
                sceneState.enqueueMoveAnimation(move, renderState)
            }
            requestRender()
            return
        }
        sceneState.enqueueMoveAnimation(move, renderState)
    }

    /** When true, drag gestures are ignored. Pinch zoom remains active. */
    fun setRotationLocked(locked: Boolean) {
        if (rotationLocked == locked) return
        rotationLocked = locked
        if (locked) {
            touchController.onCancel()
        }
    }

    private fun cancelActiveInteraction() {
        touchController.onCancel()
        pinchZoomController.onCancel()
    }

    private fun handlePinchMove(event: MotionEvent) {
        val span = currentPinchSpan(event)
        queueEvent {
            val zoom = pinchZoomController.onMove(
                span = span,
                currentZoom = sceneState.camera.targetZoom,
                minZoom = sceneState.minZoom,
                maxZoom = sceneState.maxZoom,
                sensitivity = RubikSceneConfiguration.PINCH_ZOOM_SENSITIVITY,
            )
            sceneState.camera.setTargetZoom(zoom)
        }
    }

    private fun handleSinglePointerMove(event: MotionEvent) {
        val activePointerId = touchController.activePointerId ?: return
        val pointerIndex = event.findPointerIndex(activePointerId)
        if (pointerIndex < 0) {
            touchController.onCancel()
            return
        }

        touchController.onMove(
            pointerCount = event.pointerCount,
            pointerId = activePointerId,
            x = event.getX(pointerIndex),
            y = event.getY(pointerIndex),
        )
    }

    private fun currentPinchSpan(event: MotionEvent): Float {
        if (event.pointerCount < 2) {
            return 0f
        }
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    private fun applyViewStyle(style: RubikCubeViewStyle) {
        appearance = style.appearance
        sceneState.updateZoomSettings(style.zoomSettings)
        doubleTapResetEnabled = style.doubleTapResetEnabled
    }
}
