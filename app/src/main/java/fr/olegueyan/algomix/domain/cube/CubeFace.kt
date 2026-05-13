package fr.olegueyan.algomix.domain.cube

/** Faces ordered to match the face-color arrays stored on each [Cubie]. */
enum class CubeFace(val nx: Int, val ny: Int, val nz: Int) {
    RIGHT(1, 0, 0),
    LEFT(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    FRONT(0, 0, 1),
    BACK(0, 0, -1),
}
