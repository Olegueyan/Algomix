package fr.olegueyan.algomix.domain.cube

enum class FaceletFace(
    val normal: CubeVector,
    val solvedColor: FaceColor,
) {
    UP(CubeVector(0, 1, 0), FaceColor.YELLOW),
    DOWN(CubeVector(0, -1, 0), FaceColor.WHITE),
    RIGHT(CubeVector(1, 0, 0), FaceColor.BLUE),
    LEFT(CubeVector(-1, 0, 0), FaceColor.GREEN),
    FRONT(CubeVector(0, 0, 1), FaceColor.ORANGE),
    BACK(CubeVector(0, 0, -1), FaceColor.RED),
}

data class FaceletCube(
    val stickers: List<FaceColor?>,
) {
    fun stickerAt(face: FaceletFace, row: Int, column: Int): FaceColor? =
        stickers.getOrNull(face.ordinal * FACE_STICKER_COUNT + row * FACE_SIZE + column)

    companion object {
        const val FACE_SIZE = 3
        const val FACE_STICKER_COUNT = FACE_SIZE * FACE_SIZE
        val STICKER_COUNT = FaceletFace.entries.size * FACE_STICKER_COUNT
    }
}

data class CubeState internal constructor(
    internal val cubelets: List<Cubelet>,
) {
    val faceletCube: FaceletCube
        get() = FaceletCube(
            FaceletFace.entries.flatMap { face ->
                faceCoordinates(face).map { coordinate ->
                    cubelets
                        .firstOrNull { it.position == coordinate }
                        ?.stickers
                        ?.get(face.normal)
                }
            },
        )

    companion object {
        fun solved(): CubeState {
            val cubelets = mutableListOf<Cubelet>()
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        val position = CubeVector(x, y, z)
                        val stickers = FaceletFace.entries
                            .filter { face -> position.hasOuterSticker(face.normal) }
                            .associate { face -> face.normal to face.solvedColor }
                        cubelets += Cubelet(position, stickers)
                    }
                }
            }
            return CubeState(cubelets.sortedByPosition())
        }

        fun fromFaceletCube(faceletCube: FaceletCube): CubeState {
            val validation = CubeValidator.validate(faceletCube)
            require(validation.isValid) { validation.errors.joinToString("; ") }
            val stickersByCoordinate = mutableMapOf<CubeVector, MutableMap<CubeVector, FaceColor>>()
            FaceletFace.entries.forEach { face ->
                faceCoordinates(face).forEachIndexed { index, coordinate ->
                    val color = faceletCube.stickers.getOrNull(face.ordinal * FaceletCube.FACE_STICKER_COUNT + index)
                    if (color != null) {
                        stickersByCoordinate
                            .getOrPut(coordinate) { mutableMapOf() }[face.normal] = color
                    }
                }
            }
            val cubelets = mutableListOf<Cubelet>()
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        val position = CubeVector(x, y, z)
                        cubelets += Cubelet(position, stickersByCoordinate[position].orEmpty())
                    }
                }
            }
            return CubeState(cubelets.sortedByPosition())
        }

        internal fun fromCubelets(cubelets: List<Cubelet>): CubeState =
            CubeState(cubelets.sortedByPosition())
    }
}

internal data class Cubelet(
    val position: CubeVector,
    val stickers: Map<CubeVector, FaceColor>,
)

data class CubeVector(
    val x: Int,
    val y: Int,
    val z: Int,
)

private fun List<Cubelet>.sortedByPosition(): List<Cubelet> =
    sortedWith(compareBy<Cubelet> { it.position.x }.thenBy { it.position.y }.thenBy { it.position.z })

internal fun faceCoordinates(face: FaceletFace): List<CubeVector> =
    when (face) {
        FaceletFace.UP -> grid { row, column -> CubeVector(column - 1, 1, row - 1) }
        FaceletFace.DOWN -> grid { row, column -> CubeVector(column - 1, -1, 1 - row) }
        FaceletFace.RIGHT -> grid { row, column -> CubeVector(1, 1 - row, 1 - column) }
        FaceletFace.LEFT -> grid { row, column -> CubeVector(-1, 1 - row, column - 1) }
        FaceletFace.FRONT -> grid { row, column -> CubeVector(column - 1, 1 - row, 1) }
        FaceletFace.BACK -> grid { row, column -> CubeVector(1 - column, 1 - row, -1) }
    }

private fun grid(block: (row: Int, column: Int) -> CubeVector): List<CubeVector> =
    (0 until FaceletCube.FACE_SIZE).flatMap { row ->
        (0 until FaceletCube.FACE_SIZE).map { column -> block(row, column) }
    }

private fun CubeVector.hasOuterSticker(normal: CubeVector): Boolean =
    (normal.x != 0 && x == normal.x) ||
        (normal.y != 0 && y == normal.y) ||
        (normal.z != 0 && z == normal.z)
