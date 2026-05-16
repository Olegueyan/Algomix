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
        return CubeValidation(errors.isEmpty(), errors)
    }

    fun isSolved(state: CubeState): Boolean = state == CubeState.solved()
}

data class CubeValidation(
    val isValid: Boolean,
    val errors: List<String>,
)
