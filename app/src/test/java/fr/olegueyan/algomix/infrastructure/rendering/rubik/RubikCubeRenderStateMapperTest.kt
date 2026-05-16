package fr.olegueyan.algomix.infrastructure.rendering.rubik

import fr.olegueyan.algomix.domain.cube.CubeFace
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.cube.RubikCubeConstants
import fr.olegueyan.algomix.domain.cube.RubikCubeState
import org.junit.Assert.assertEquals
import org.junit.Test

class RubikCubeRenderStateMapperTest {
    @Test
    fun mapsSolvedDomainCubeToCurrentRenderConvention() {
        val mapped = RubikCubeRenderStateMapper.map(CubeState.solved())
        val expected = RubikCubeState()

        for (x in 0 until RubikCubeConstants.ORDER) {
            for (y in 0 until RubikCubeConstants.ORDER) {
                for (z in 0 until RubikCubeConstants.ORDER) {
                    val mappedCubie = mapped.cubieAt(x, y, z)
                    val expectedCubie = expected.cubieAt(x, y, z)
                    assertEquals(expectedCubie?.faces?.toList(), mappedCubie?.faces?.toList())
                }
            }
        }
    }

    @Test
    fun mapsExternalFaceColorsToRendererFaces() {
        val mapped = RubikCubeRenderStateMapper.map(CubeState.solved())

        assertEquals(FaceColor.BLUE, mapped.cubieAt(2, 1, 1)?.faces?.get(CubeFace.RIGHT.ordinal))
        assertEquals(FaceColor.GREEN, mapped.cubieAt(0, 1, 1)?.faces?.get(CubeFace.LEFT.ordinal))
        assertEquals(FaceColor.YELLOW, mapped.cubieAt(1, 2, 1)?.faces?.get(CubeFace.UP.ordinal))
        assertEquals(FaceColor.WHITE, mapped.cubieAt(1, 0, 1)?.faces?.get(CubeFace.DOWN.ordinal))
        assertEquals(FaceColor.ORANGE, mapped.cubieAt(1, 1, 2)?.faces?.get(CubeFace.FRONT.ordinal))
        assertEquals(FaceColor.RED, mapped.cubieAt(1, 1, 0)?.faces?.get(CubeFace.BACK.ordinal))
    }
}
