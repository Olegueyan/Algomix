package fr.olegueyan.algomix.domain.cube

/** Mutable 3x3x3 cube state used by the renderer. */
class RubikCubeState {
    val cubies = mutableListOf<Cubie>()

    init {
        resetToSolved()
    }

    /** Restores the cube to the solved state with the default sticker orientation. */
    fun resetToSolved() {
        cubies.clear()
        for (x in 0 until RubikCubeConstants.ORDER) {
            for (y in 0 until RubikCubeConstants.ORDER) {
                for (z in 0 until RubikCubeConstants.ORDER) {
                    cubies += Cubie(x, y, z, createSolvedFaces(x, y, z))
                }
            }
        }
    }

    /** Returns the cubie at the given grid position, or `null` if none matches. */
    fun cubieAt(x: Int, y: Int, z: Int): Cubie? =
        cubies.firstOrNull { it.gx == x && it.gy == y && it.gz == z }

    /** Builds the sticker array for a solved cubie at the given grid coordinates. */
    private fun createSolvedFaces(x: Int, y: Int, z: Int): Array<FaceColor?> {
        val faces = arrayOfNulls<FaceColor>(CubeFace.entries.size)
        if (x == RubikCubeConstants.LAST_INDEX) {
            faces[CubeFace.RIGHT.ordinal] = FaceColor.BLUE
        }
        if (x == 0) {
            faces[CubeFace.LEFT.ordinal] = FaceColor.GREEN
        }
        if (y == RubikCubeConstants.LAST_INDEX) {
            faces[CubeFace.UP.ordinal] = FaceColor.YELLOW
        }
        if (y == 0) {
            faces[CubeFace.DOWN.ordinal] = FaceColor.WHITE
        }
        if (z == RubikCubeConstants.LAST_INDEX) {
            faces[CubeFace.FRONT.ordinal] = FaceColor.ORANGE
        }
        if (z == 0) {
            faces[CubeFace.BACK.ordinal] = FaceColor.RED
        }
        return faces
    }
}
