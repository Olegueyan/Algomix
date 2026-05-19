package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.FaceColor
import kotlin.math.roundToInt

data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
)

object ScanColorClassifier {
    fun classify(color: RgbColor): FaceColor =
        FaceColor.entries.minBy { faceColor ->
            squaredDistance(color, faceColor.referenceRgb())
        }

    fun classifyCells(cells: List<RgbColor>): List<FaceColor> =
        cells.map(::classify)

    private fun squaredDistance(left: RgbColor, right: RgbColor): Int {
        val red = left.red - right.red
        val green = left.green - right.green
        val blue = left.blue - right.blue
        return red * red + green * green + blue * blue
    }

    private fun FaceColor.referenceRgb(): RgbColor =
        RgbColor(
            red = (r * RGB_MAX).roundToInt().coerceIn(0, RGB_MAX),
            green = (g * RGB_MAX).roundToInt().coerceIn(0, RGB_MAX),
            blue = (b * RGB_MAX).roundToInt().coerceIn(0, RGB_MAX),
        )

    private const val RGB_MAX = 255
}
