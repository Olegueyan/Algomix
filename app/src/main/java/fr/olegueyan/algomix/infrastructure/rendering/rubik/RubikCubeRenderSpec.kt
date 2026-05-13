package fr.olegueyan.algomix.infrastructure.rendering.rubik

import fr.olegueyan.algomix.domain.cube.RubikCubeConstants
import kotlin.math.sqrt

internal object RubikCubeRenderSpec {
    private const val CUBE_SCALE = 0.68f
    private const val CUBIE_BODY_SIZE = 0.96f
    private const val CORE_HALF_SIZE = 1.47f
    private const val STICKER_FACE_SCALE = 0.82f
    private const val STICKER_LIFT = 0.003f

    const val cubieHalfExtent = CUBIE_BODY_SIZE * CUBE_SCALE / 2f
    const val coreHalfExtent = CORE_HALF_SIZE * CUBE_SCALE
    const val stickerHalfExtent = cubieHalfExtent * STICKER_FACE_SCALE
    const val stickerOffset = cubieHalfExtent + STICKER_LIFT - stickerHalfExtent
    private const val outerLayerOffset = RubikCubeConstants.CENTER_INDEX * RubikCubeConstants.CUBIE_STRIDE * CUBE_SCALE
    const val outerExtent = outerLayerOffset + stickerOffset + stickerHalfExtent
    val boundingRadius: Float = sqrt(3f * outerExtent * outerExtent)

    fun cubeOffset(gridCoordinate: Int): Float =
        (gridCoordinate - RubikCubeConstants.CENTER_INDEX) * RubikCubeConstants.CUBIE_STRIDE * CUBE_SCALE
}
