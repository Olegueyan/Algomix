package fr.olegueyan.algomix.application.rubik.scene

import fr.olegueyan.algomix.domain.cube.Move
import fr.olegueyan.algomix.domain.cube.MoveAxis
import fr.olegueyan.algomix.domain.cube.MoveKind
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Camera orientation as a unit quaternion (qx, qy, qz, qw).
 */
data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
    }
}

/**
 * Computes the camera orientation a double-tap reset should snap back to.
 *
 * For Visualization mode the target is the bare isometric pose. For Free, Play and Edit modes the
 * pose is the iso pose composed with the cumulative effect of every rotation move (`x`, `y`, `z`)
 * that the sequence has applied so far — so the cube stays "naturally oriented" after the algorithm
 * rotates it.
 */
object RubikResetTarget {

    fun isoQuaternion(
        isoXDeg: Float = RubikSceneConfiguration.ISO_X_DEG,
        isoYDeg: Float = RubikSceneConfiguration.ISO_Y_DEG,
    ): Quaternion = composeIsoQuaternion(isoXDeg, isoYDeg)

    fun composeRotationMoves(
        rotationMoves: List<Move>,
        isoXDeg: Float = RubikSceneConfiguration.ISO_X_DEG,
        isoYDeg: Float = RubikSceneConfiguration.ISO_Y_DEG,
    ): Quaternion {
        var current = isoQuaternion(isoXDeg, isoYDeg)
        for (move in rotationMoves) {
            if (move.kind != MoveKind.ROTATION) continue
            current = current.multiply(quaternionFor(move))
        }
        return current.normalize()
    }

    private fun quaternionFor(move: Move): Quaternion {
        val angleDeg = move.effectiveQuarterTurns * QUARTER_TURN_DEGREES
        return axisAngle(move.axis, angleDeg)
    }

    private fun axisAngle(axis: MoveAxis, angleDeg: Float): Quaternion {
        val half = Math.toRadians(angleDeg.toDouble() / 2f).toFloat()
        val s = sin(half)
        val c = cos(half)
        return when (axis) {
            MoveAxis.X -> Quaternion(s, 0f, 0f, c)
            MoveAxis.Y -> Quaternion(0f, s, 0f, c)
            MoveAxis.Z -> Quaternion(0f, 0f, s, c)
        }
    }

    private fun composeIsoQuaternion(isoXDeg: Float, isoYDeg: Float): Quaternion {
        val hx = Math.toRadians(isoXDeg.toDouble()).toFloat() / 2f
        val hy = Math.toRadians(isoYDeg.toDouble()).toFloat() / 2f
        val sx = sin(hx)
        val cx = cos(hx)
        val sy = sin(hy)
        val cy = cos(hy)
        return Quaternion(sx * cy, cx * sy, sx * sy, cx * cy).normalize()
    }

    private fun Quaternion.multiply(rhs: Quaternion): Quaternion = Quaternion(
        w * rhs.x + x * rhs.w + y * rhs.z - z * rhs.y,
        w * rhs.y - x * rhs.z + y * rhs.w + z * rhs.x,
        w * rhs.z + x * rhs.y - y * rhs.x + z * rhs.w,
        w * rhs.w - x * rhs.x - y * rhs.y - z * rhs.z,
    )

    private fun Quaternion.normalize(): Quaternion {
        val magnitude = sqrt(x * x + y * y + z * z + w * w)
        if (magnitude < 1e-6f) return Quaternion.IDENTITY
        return Quaternion(x / magnitude, y / magnitude, z / magnitude, w / magnitude)
    }

    private const val QUARTER_TURN_DEGREES = 90f
}
