package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.FaceColor
import kotlin.math.abs
import kotlin.math.roundToInt

data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
)

object ScanColorClassifier {
    fun classify(color: RgbColor): FaceColor =
        FaceColor.entries.minBy { hsvDistance(rgbToHsv(color), it.referenceHsv()) }

    fun classifyCells(cells: List<RgbColor>): List<FaceColor> =
        cells.map(::classify)

    // Pure-Kotlin RGB→HSV: no Android dependency, safe in domain layer.
    // Returns [hue 0-360°, saturation 0-1, value 0-1].
    internal fun rgbToHsv(color: RgbColor): FloatArray {
        val r = color.red / RGB_MAX_F
        val g = color.green / RGB_MAX_F
        val b = color.blue / RGB_MAX_F
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val v = max
        val s = if (max == 0f) 0f else delta / max
        val rawH = when {
            delta == 0f -> 0f
            max == r    -> 60f * ((g - b) / delta % 6f)
            max == g    -> 60f * ((b - r) / delta + 2f)
            else        -> 60f * ((r - g) / delta + 4f)
        }
        val h = if (rawH < 0f) rawH + 360f else rawH
        return floatArrayOf(h, s, v)
    }

    // Weighted HSV distance.
    // Hue carries the color identity, so it gets the highest weight.
    // Red wraps around 360°: hueDelta handles the shortest arc.
    // Achromatic colours (white, near-white) are identified by low saturation
    // before the hue calculation to avoid undefined hue affecting the result.
    private fun hsvDistance(a: FloatArray, b: FloatArray): Float {
        if (b[SAT] < SAT_WHITE_THRESHOLD) {
            // Reference is white — match only if input is also achromatic
            return if (a[SAT] < SAT_WHITE_THRESHOLD) abs(a[VAL] - b[VAL]) else LARGE_DIST
        }
        val dh = hueDelta(a[HUE], b[HUE]) / HUE_NORM
        val ds = abs(a[SAT] - b[SAT])
        val dv = abs(a[VAL] - b[VAL])
        return dh * HUE_WEIGHT + ds * SAT_WEIGHT + dv * VAL_WEIGHT
    }

    private fun hueDelta(h1: Float, h2: Float): Float {
        val d = abs(h1 - h2)
        return if (d > 180f) 360f - d else d
    }

    private fun FaceColor.referenceHsv(): FloatArray =
        rgbToHsv(referenceRgb())

    private fun FaceColor.referenceRgb(): RgbColor =
        RgbColor(
            red = (r * RGB_MAX_F).roundToInt().coerceIn(0, RGB_MAX),
            green = (g * RGB_MAX_F).roundToInt().coerceIn(0, RGB_MAX),
            blue = (b * RGB_MAX_F).roundToInt().coerceIn(0, RGB_MAX),
        )

    private const val HUE = 0
    private const val SAT = 1
    private const val VAL = 2
    private const val RGB_MAX = 255
    private const val RGB_MAX_F = 255f
    private const val HUE_NORM = 180f  // normalise dh to 0-1 range over half circle
    private const val SAT_WHITE_THRESHOLD = 0.20f
    private const val LARGE_DIST = 999f
    private const val HUE_WEIGHT = 2.0f
    private const val SAT_WEIGHT = 1.0f
    private const val VAL_WEIGHT = 0.5f
}
