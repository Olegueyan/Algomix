package fr.olegueyan.algomix.domain.cube

object CubeValidator {
    fun validate(cube: FaceletCube): CubeValidation {
        val errors = mutableListOf<String>()
        if (cube.stickers.size != FaceletCube.STICKER_COUNT) {
            errors += "Cube must contain ${FaceletCube.STICKER_COUNT} stickers"
        }
        if (cube.stickers.any { it == null }) {
            errors += "Cube contains missing stickers"
        }
        val counts = cube.stickers.filterNotNull().groupingBy { it }.eachCount()
        for (color in FaceColor.entries) {
            if ((counts[color] ?: 0) != FaceletCube.FACE_STICKER_COUNT) {
                errors += "Color $color must appear ${FaceletCube.FACE_STICKER_COUNT} times"
            }
        }
        if (cube.stickers.size == FaceletCube.STICKER_COUNT && cube.stickers.none { it == null }) {
            errors += validateCenters(cube)
            errors += validateCubieColorSets(cube)
        }
        return CubeValidation(errors.isEmpty(), errors)
    }

    fun isSolved(state: CubeState): Boolean = state == CubeState.solved()

    private fun validateCenters(cube: FaceletCube): List<String> {
        val centers = FaceletFace.entries.mapNotNull { face ->
            cube.stickers.getOrNull(face.ordinal * FaceletCube.FACE_STICKER_COUNT + CENTER_INDEX)
        }
        return if (centers.size == FaceletFace.entries.size && centers.toSet().size == FaceletFace.entries.size) {
            emptyList()
        } else {
            listOf("Cube centers must contain six unique colors")
        }
    }

    private fun validateCubieColorSets(cube: FaceletCube): List<String> {
        val expected = CubieColorSets.from(CubeState.solved().faceletCube)
        val actual = CubieColorSets.from(cube)
        val errors = mutableListOf<String>()
        if (actual.edges != expected.edges) {
            errors += "Edge color sets are inconsistent"
        }
        if (actual.corners != expected.corners) {
            errors += "Corner color sets are inconsistent"
        }
        return errors
    }

    private data class CubieColorSets(
        val edges: Map<Set<FaceColor>, Int>,
        val corners: Map<Set<FaceColor>, Int>,
    ) {
        companion object {
            fun from(cube: FaceletCube): CubieColorSets {
                val colorsByCoordinate = mutableMapOf<CubeVector, MutableSet<FaceColor>>()
                FaceletFace.entries.forEach { face ->
                    faceCoordinates(face).forEachIndexed { index, coordinate ->
                        val stickerIndex = face.ordinal * FaceletCube.FACE_STICKER_COUNT + index
                        val color = cube.stickers.getOrNull(stickerIndex)
                        if (color != null) {
                            colorsByCoordinate.getOrPut(coordinate) { mutableSetOf() } += color
                        }
                    }
                }
                val edges = colorsByCoordinate
                    .filterKeys { coordinate -> coordinate.outerStickerCount() == EDGE_STICKER_COUNT }
                    .values
                    .map { colors -> colors.toSet() }
                    .groupingBy { it }
                    .eachCount()
                val corners = colorsByCoordinate
                    .filterKeys { coordinate -> coordinate.outerStickerCount() == CORNER_STICKER_COUNT }
                    .values
                    .map { colors -> colors.toSet() }
                    .groupingBy { it }
                    .eachCount()
                return CubieColorSets(edges, corners)
            }
        }
    }

    private fun CubeVector.outerStickerCount(): Int =
        listOf(x, y, z).count { coordinate -> coordinate != 0 }

    private const val CENTER_INDEX = 4
    private const val EDGE_STICKER_COUNT = 2
    private const val CORNER_STICKER_COUNT = 3
}

data class CubeValidation(
    val isValid: Boolean,
    val errors: List<String>,
)
