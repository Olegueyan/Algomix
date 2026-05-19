package fr.olegueyan.algomix.domain.session

import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.MoveExecutor
import fr.olegueyan.algomix.domain.cube.MoveParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CubeSessionCodecTest {
    @Test
    fun roundTripsSolvedAndMovedCubeStates() {
        val solved = CubeState.solved()
        val moved = MoveExecutor.apply(solved, MoveParser.parse("R U R' U'"))

        assertEquals(solved, CubeSessionCodec.decode(CubeSessionCodec.encode(solved)))
        assertEquals(moved, CubeSessionCodec.decode(CubeSessionCodec.encode(moved)))
    }

    @Test
    fun invalidSnapshotFallsBackToNull() {
        assertNull(CubeSessionCodec.decode(null))
        assertNull(CubeSessionCodec.decode("invalid"))
        assertNull(CubeSessionCodec.decode(List(54) { "WHITE" }.joinToString(",")))
    }
}
