package fr.olegueyan.algomix.domain.cube

/** Sticker colors in the solved cube convention used by the app. */
enum class FaceColor(val r: Float, val g: Float, val b: Float) {
    WHITE(1f, 1f, 1f),
    YELLOW(1f, 0.84f, 0f),
    RED(0.77f, 0.12f, 0.23f),
    ORANGE(1f, 0.47f, 0f),
    BLUE(0f, 0.318f, 0.729f),
    GREEN(0f, 0.62f, 0.38f),
    ;

    /** Precomputed RGBA vector used directly by the OpenGL renderer. */
    val rgba = floatArrayOf(r, g, b, 1f)
}
