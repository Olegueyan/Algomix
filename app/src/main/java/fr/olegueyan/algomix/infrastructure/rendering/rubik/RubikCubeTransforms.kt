package fr.olegueyan.algomix.infrastructure.rendering.rubik

import android.opengl.Matrix
import fr.olegueyan.algomix.domain.cube.CubeFace
import fr.olegueyan.algomix.domain.cube.Cubie

internal object RubikCubeTransforms {
    fun setCore(modelMatrix: FloatArray) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(
            modelMatrix,
            0,
            RubikCubeRenderSpec.coreHalfExtent,
            RubikCubeRenderSpec.coreHalfExtent,
            RubikCubeRenderSpec.coreHalfExtent,
        )
    }

    fun setCubieBody(modelMatrix: FloatArray, cubie: Cubie) {
        setCubieBase(modelMatrix, cubie)
        Matrix.scaleM(
            modelMatrix,
            0,
            RubikCubeRenderSpec.cubieHalfExtent,
            RubikCubeRenderSpec.cubieHalfExtent,
            RubikCubeRenderSpec.cubieHalfExtent,
        )
    }

    fun setSticker(modelMatrix: FloatArray, cubie: Cubie, face: CubeFace) {
        setCubieBase(modelMatrix, cubie)
        Matrix.translateM(
            modelMatrix,
            0,
            face.nx * RubikCubeRenderSpec.stickerOffset,
            face.ny * RubikCubeRenderSpec.stickerOffset,
            face.nz * RubikCubeRenderSpec.stickerOffset,
        )
        Matrix.scaleM(
            modelMatrix,
            0,
            RubikCubeRenderSpec.stickerHalfExtent,
            RubikCubeRenderSpec.stickerHalfExtent,
            RubikCubeRenderSpec.stickerHalfExtent,
        )
    }

    private fun setCubieBase(modelMatrix: FloatArray, cubie: Cubie) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(
            modelMatrix,
            0,
            RubikCubeRenderSpec.cubeOffset(cubie.gx),
            RubikCubeRenderSpec.cubeOffset(cubie.gy),
            RubikCubeRenderSpec.cubeOffset(cubie.gz),
        )
    }
}
