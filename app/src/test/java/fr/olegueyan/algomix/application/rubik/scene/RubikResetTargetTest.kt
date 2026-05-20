package fr.olegueyan.algomix.application.rubik.scene

import fr.olegueyan.algomix.domain.cube.MoveParser
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class RubikResetTargetTest {

    @Test
    fun isoQuaternionIsAUnitQuaternion() {
        val q = RubikResetTarget.isoQuaternion()
        val magnitude = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w
        assertEquals(1f, magnitude, EPSILON)
    }

    @Test
    fun composingEmptyRotationListReturnsIso() {
        val iso = RubikResetTarget.isoQuaternion()
        val result = RubikResetTarget.composeRotationMoves(emptyList())
        assertEquals(iso.x, result.x, EPSILON)
        assertEquals(iso.y, result.y, EPSILON)
        assertEquals(iso.z, result.z, EPSILON)
        assertEquals(iso.w, result.w, EPSILON)
    }

    @Test
    fun composingFaceTurnsAloneDoesNotShiftTheCamera() {
        val moves = MoveParser.parse("R U R' U'").moves
        val iso = RubikResetTarget.isoQuaternion()
        val result = RubikResetTarget.composeRotationMoves(moves)
        assertEquals(iso.x, result.x, EPSILON)
        assertEquals(iso.y, result.y, EPSILON)
        assertEquals(iso.z, result.z, EPSILON)
        assertEquals(iso.w, result.w, EPSILON)
    }

    @Test
    fun composingRotationMoveShiftsAwayFromIsoOrientation() {
        val moves = MoveParser.parse("x").moves
        val iso = RubikResetTarget.isoQuaternion()
        val result = RubikResetTarget.composeRotationMoves(moves)
        val differenceMagnitude = abs(iso.x - result.x) + abs(iso.y - result.y) +
            abs(iso.z - result.z) + abs(iso.w - result.w)
        assertEquals(true, differenceMagnitude > 0.1f)
    }

    @Test
    fun composingResultRemainsUnitQuaternion() {
        val moves = MoveParser.parse("x y z' y2").moves
        val result = RubikResetTarget.composeRotationMoves(moves)
        val magnitude = result.x * result.x + result.y * result.y +
            result.z * result.z + result.w * result.w
        assertEquals(1f, magnitude, EPSILON)
    }

    private companion object {
        const val EPSILON = 1e-4f
    }
}
