package fr.olegueyan.algomix.domain.cube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RubikCubeStateTest {
    @Test
    fun solvedCubeContains27Cubies() {
        val cubeState = RubikCubeState()

        assertEquals(27, cubeState.cubies.size)
    }

    @Test
    fun externalFacesReceiveExpectedColors() {
        val cubeState = RubikCubeState()

        for (cubie in cubeState.cubies) {
            assertFace(cubie, cubie.gx == RubikCubeConstants.LAST_INDEX, CubeFace.RIGHT, FaceColor.BLUE)
            assertFace(cubie, cubie.gx == 0, CubeFace.LEFT, FaceColor.GREEN)
            assertFace(cubie, cubie.gy == RubikCubeConstants.LAST_INDEX, CubeFace.UP, FaceColor.YELLOW)
            assertFace(cubie, cubie.gy == 0, CubeFace.DOWN, FaceColor.WHITE)
            assertFace(cubie, cubie.gz == RubikCubeConstants.LAST_INDEX, CubeFace.FRONT, FaceColor.ORANGE)
            assertFace(cubie, cubie.gz == 0, CubeFace.BACK, FaceColor.RED)
        }
    }

    @Test
    fun internalCubieHasNoSticker() {
        val cubeState = RubikCubeState()

        val coreCubie = cubeState.cubieAt(1, 1, 1)
        assertNotNull(coreCubie)
        assertTrue(coreCubie!!.faces.all { it == null })
    }

    private fun assertFace(cubie: Cubie, shouldBeColored: Boolean, face: CubeFace, expectedColor: FaceColor) {
        if (shouldBeColored) {
            assertEquals(expectedColor, cubie.faces[face.ordinal])
        } else {
            assertNull(cubie.faces[face.ordinal])
        }
    }
}
