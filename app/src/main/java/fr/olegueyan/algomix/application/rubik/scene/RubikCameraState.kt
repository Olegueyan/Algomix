package fr.olegueyan.algomix.application.rubik.scene

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Camera state for the cube viewer.
 *
 * Orientation is stored as a quaternion so drag rotations remain stable and can be interpolated
 * back to the isometric resting pose without gimbal lock.
 */
internal class RubikCameraState {
    var qx = 0f
        private set
    var qy = 0f
        private set
    var qz = 0f
        private set
    var qw = 1f
        private set

    private var currentZoom = 1f
    private var minZoom = 1f
    private var maxZoom = Float.MAX_VALUE

    var zoom: Float
        get() = currentZoom
        set(value) {
            currentZoom = value.coerceIn(minZoom, maxZoom)
            targetZoom = currentZoom
        }

    var targetZoom = 1f
        private set

    init {
        resetToIso(zoom = currentZoom)
    }

    fun resetToIso(
        isoXDeg: Float = RubikSceneConfiguration.ISO_X_DEG,
        isoYDeg: Float = RubikSceneConfiguration.ISO_Y_DEG,
        zoom: Float = currentZoom,
    ) {
        fillIsoQuaternion(isoXDeg, isoYDeg)
        this.zoom = zoom
    }

    /** Writes the current quaternion to a 4x4 rotation matrix compatible with OpenGL. */
    fun toMatrix(matrix: FloatArray) {
        matrix[0] = 1 - 2 * (qy * qy + qz * qz)
        matrix[4] = 2 * (qx * qy - qw * qz)
        matrix[8] = 2 * (qx * qz + qw * qy)
        matrix[12] = 0f

        matrix[1] = 2 * (qx * qy + qw * qz)
        matrix[5] = 1 - 2 * (qx * qx + qz * qz)
        matrix[9] = 2 * (qy * qz - qw * qx)
        matrix[13] = 0f

        matrix[2] = 2 * (qx * qz - qw * qy)
        matrix[6] = 2 * (qy * qz + qw * qx)
        matrix[10] = 1 - 2 * (qx * qx + qy * qy)
        matrix[14] = 0f

        matrix[3] = 0f
        matrix[7] = 0f
        matrix[11] = 0f
        matrix[15] = 1f
    }

    fun drag(dx: Float, dy: Float, sensitivity: Float = RubikSceneConfiguration.DRAG_SENSITIVITY) {
        val halfSensitivity = sensitivity / 2f
        val pitch = dy * halfSensitivity
        val yaw = dx * halfSensitivity
        multiplyLeft(sin(pitch), 0f, 0f, cos(pitch))
        multiplyLeft(0f, sin(yaw), 0f, cos(yaw))
        normalize()
    }

    fun updateZoomBounds(minZoom: Float, maxZoom: Float, resetZoom: Boolean = false) {
        this.minZoom = minZoom.coerceAtMost(maxZoom)
        this.maxZoom = maxZoom.coerceAtLeast(minZoom)
        if (resetZoom) {
            zoom = this.minZoom
            return
        }
        clampZoom()
    }

    fun setTargetZoom(zoom: Float) {
        targetZoom = zoom.coerceIn(minZoom, maxZoom)
    }

    fun clampZoom() {
        currentZoom = currentZoom.coerceIn(minZoom, maxZoom)
        targetZoom = targetZoom.coerceIn(minZoom, maxZoom)
    }

    fun animateZoom(amount: Float) {
        currentZoom += (targetZoom - currentZoom) * amount.coerceIn(0.05f, 1f)
        if (abs(targetZoom - currentZoom) < 0.001f) {
            currentZoom = targetZoom
        }
    }

    fun slerpOrientationToIso(
        amount: Float,
        isoXDeg: Float = RubikSceneConfiguration.ISO_X_DEG,
        isoYDeg: Float = RubikSceneConfiguration.ISO_Y_DEG,
    ) {
        val target = isoQuaternion(isoXDeg, isoYDeg)
        slerpOrientationTo(amount, target[0], target[1], target[2], target[3])
    }

    fun slerpOrientationTo(amount: Float, tx: Float, ty: Float, tz: Float, tw: Float) {
        val dot = qx * tx + qy * ty + qz * tz + qw * tw
        val signedTx = if (dot < 0f) -tx else tx
        val signedTy = if (dot < 0f) -ty else ty
        val signedTz = if (dot < 0f) -tz else tz
        val signedTw = if (dot < 0f) -tw else tw
        qx += (signedTx - qx) * amount
        qy += (signedTy - qy) * amount
        qz += (signedTz - qz) * amount
        qw += (signedTw - qw) * amount
        normalize()
    }

    fun isOrientationNearIso(
        threshold: Float,
        isoXDeg: Float = RubikSceneConfiguration.ISO_X_DEG,
        isoYDeg: Float = RubikSceneConfiguration.ISO_Y_DEG,
    ): Boolean {
        val target = isoQuaternion(isoXDeg, isoYDeg)
        return isOrientationNear(threshold, target[0], target[1], target[2], target[3])
    }

    fun isOrientationNear(threshold: Float, tx: Float, ty: Float, tz: Float, tw: Float): Boolean {
        val dot = abs(qx * tx + qy * ty + qz * tz + qw * tw)
        return dot > threshold
    }

    private fun fillIsoQuaternion(isoXDeg: Float, isoYDeg: Float) {
        val target = isoQuaternion(isoXDeg, isoYDeg)
        qx = target[0]
        qy = target[1]
        qz = target[2]
        qw = target[3]
    }

    private fun isoQuaternion(isoXDeg: Float, isoYDeg: Float): FloatArray {
        val hx = Math.toRadians(isoXDeg.toDouble()).toFloat() / 2f
        val hy = Math.toRadians(isoYDeg.toDouble()).toFloat() / 2f
        val sx = sin(hx)
        val cx = cos(hx)
        val sy = sin(hy)
        val cy = cos(hy)
        val length = sqrt(
            (sx * cy) * (sx * cy) +
                (cx * sy) * (cx * sy) +
                (sx * sy) * (sx * sy) +
                (cx * cy) * (cx * cy)
        )
        return floatArrayOf(sx * cy / length, cx * sy / length, sx * sy / length, cx * cy / length)
    }

    private fun multiplyLeft(lx: Float, ly: Float, lz: Float, lw: Float) {
        val rx = qx
        val ry = qy
        val rz = qz
        val rw = qw
        qx = lw * rx + lx * rw + ly * rz - lz * ry
        qy = lw * ry - lx * rz + ly * rw + lz * rx
        qz = lw * rz + lx * ry - ly * rx + lz * rw
        qw = lw * rw - lx * rx - ly * ry - lz * rz
    }

    private fun normalize() {
        val magnitude = sqrt(qx * qx + qy * qy + qz * qz + qw * qw)
        if (magnitude > 1e-6f) {
            qx /= magnitude
            qy /= magnitude
            qz /= magnitude
            qw /= magnitude
        }
    }
}
