package fr.olegueyan.algomix.domain.cube

/** One cube piece located on the integer grid of the solved 3x3x3 cube. */
class Cubie(
    var gx: Int,
    var gy: Int,
    var gz: Int,
    val faces: Array<FaceColor?> = arrayOfNulls(CubeFace.entries.size),
) {
    /** Returns the world-space X coordinate used by the renderer. */
    fun worldX(): Float = (gx - RubikCubeConstants.CENTER_INDEX) * RubikCubeConstants.CUBIE_STRIDE

    /** Returns the world-space Y coordinate used by the renderer. */
    fun worldY(): Float = (gy - RubikCubeConstants.CENTER_INDEX) * RubikCubeConstants.CUBIE_STRIDE

    /** Returns the world-space Z coordinate used by the renderer. */
    fun worldZ(): Float = (gz - RubikCubeConstants.CENTER_INDEX) * RubikCubeConstants.CUBIE_STRIDE
}
