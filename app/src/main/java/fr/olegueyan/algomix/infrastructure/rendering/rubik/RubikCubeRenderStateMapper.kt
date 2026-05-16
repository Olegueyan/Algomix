package fr.olegueyan.algomix.infrastructure.rendering.rubik

import fr.olegueyan.algomix.domain.cube.CubeFace
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.Cubie
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.cube.FaceletFace
import fr.olegueyan.algomix.domain.cube.RubikCubeConstants
import fr.olegueyan.algomix.domain.cube.RubikCubeState

object RubikCubeRenderStateMapper {
    fun map(cubeState: CubeState): RubikCubeState {
        val renderState = RubikCubeState()
        renderState.cubies.clear()
        for (x in 0 until RubikCubeConstants.ORDER) {
            for (y in 0 until RubikCubeConstants.ORDER) {
                for (z in 0 until RubikCubeConstants.ORDER) {
                    renderState.cubies += Cubie(x, y, z, cubeState.facesFor(x, y, z))
                }
            }
        }
        return renderState
    }

    private fun CubeState.facesFor(x: Int, y: Int, z: Int): Array<FaceColor?> {
        val faces = arrayOfNulls<FaceColor>(CubeFace.entries.size)
        val dx = x - RubikCubeConstants.CENTER_INDEX
        val dy = y - RubikCubeConstants.CENTER_INDEX
        val dz = z - RubikCubeConstants.CENTER_INDEX
        val facelets = faceletCube
        if (dx == 1) {
            faces[CubeFace.RIGHT.ordinal] = facelets.stickerAt(FaceletFace.RIGHT, row = 1 - dy, column = 1 - dz)
        }
        if (dx == -1) {
            faces[CubeFace.LEFT.ordinal] = facelets.stickerAt(FaceletFace.LEFT, row = 1 - dy, column = dz + 1)
        }
        if (dy == 1) {
            faces[CubeFace.UP.ordinal] = facelets.stickerAt(FaceletFace.UP, row = dz + 1, column = dx + 1)
        }
        if (dy == -1) {
            faces[CubeFace.DOWN.ordinal] = facelets.stickerAt(FaceletFace.DOWN, row = 1 - dz, column = dx + 1)
        }
        if (dz == 1) {
            faces[CubeFace.FRONT.ordinal] = facelets.stickerAt(FaceletFace.FRONT, row = 1 - dy, column = dx + 1)
        }
        if (dz == -1) {
            faces[CubeFace.BACK.ordinal] = facelets.stickerAt(FaceletFace.BACK, row = 1 - dy, column = 1 - dx)
        }
        return faces
    }
}
