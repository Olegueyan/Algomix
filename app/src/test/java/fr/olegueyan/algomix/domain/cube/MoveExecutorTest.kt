package fr.olegueyan.algomix.domain.cube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MoveExecutorTest {
    @Test
    fun inverseFaceMovesReturnToSolvedCube() {
        val solved = CubeState.solved()

        val result = MoveExecutor.apply(solved, MoveParser.parse("R R'"))

        assertEquals(solved, result)
    }

    @Test
    fun fourQuarterTurnsReturnToSolvedCube() {
        val solved = CubeState.solved()

        val result = MoveExecutor.apply(solved, MoveParser.parse("U U U U"))

        assertEquals(solved, result)
    }

    @Test
    fun commonTriggerChangesCube() {
        val solved = CubeState.solved()

        val result = MoveExecutor.apply(solved, MoveParser.parse("R U R' U'"))

        assertNotEquals(solved, result)
    }

    @Test
    fun wideMoveChangesCubeAndInverseRestoresIt() {
        assertMoveTypeChangesAndRestores("Rw")
    }

    @Test
    fun sliceMoveChangesCubeAndInverseRestoresIt() {
        assertMoveTypeChangesAndRestores("M")
    }

    @Test
    fun cubeRotationChangesCubeAndInverseRestoresIt() {
        assertMoveTypeChangesAndRestores("x")
    }

    private fun assertMoveTypeChangesAndRestores(token: String) {
        val solved = CubeState.solved()
        val changed = MoveExecutor.apply(solved, MoveParser.parse(token))
        val restored = MoveExecutor.apply(changed, MoveParser.parse("$token'"))

        assertNotEquals(solved, changed)
        assertEquals(solved, restored)
    }
}
