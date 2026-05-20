package fr.olegueyan.algomix.infrastructure.scan

import android.graphics.Bitmap
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CubeScanner
import fr.olegueyan.algomix.domain.cube.FaceletFace
import fr.olegueyan.algomix.domain.scan.RgbColor
import fr.olegueyan.algomix.domain.scan.ScanColorClassifier
import fr.olegueyan.algomix.domain.scan.ScanFaceDraft
import fr.olegueyan.algomix.domain.scan.ScanFaceletAssembler
import fr.olegueyan.algomix.domain.scan.ScanFaceletAssembly
import fr.olegueyan.algomix.domain.scan.ScanSessionDraft
import fr.olegueyan.algomix.domain.settings.CubeTheme
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

class CameraXCubeScanner(
    private val clockProvider: ClockProvider = SystemClockProvider,
) : CubeScanner {

    var cubeTheme: CubeTheme = CubeTheme.STICKER_ON_BLACK

    private val sampleInset: Float
        get() = when (cubeTheme) {
            CubeTheme.STICKER_ON_BLACK -> INSET_STICKER
            CubeTheme.CARBON           -> INSET_CARBON
            CubeTheme.FILLED           -> INSET_FILLED
        }

    override suspend fun startSession(): AppResult<ScanSessionDraft> =
        AppResult.success(ScanSessionDraft())

    override suspend fun saveFace(
        session: ScanSessionDraft,
        face: ScanFaceDraft,
    ): AppResult<ScanSessionDraft> {
        if (face.faceIndex !in FaceletFace.entries.indices) {
            return AppResult.failure(AppError.Validation("Face inconnue"))
        }
        if (!face.isComplete) {
            return AppResult.failure(AppError.Validation("Face incomplete"))
        }
        val faces = session.faces
            .filterNot { current -> current.faceIndex == face.faceIndex }
            .plus(face)
            .sortedBy { current -> current.faceIndex }
        return AppResult.success(ScanSessionDraft(faces = faces))
    }

    override suspend fun validateSession(session: ScanSessionDraft): AppResult<Unit> =
        ScanFaceletAssembler.assemble(session).toAppResult()

    fun extractFaceFromBitmap(bitmap: Bitmap, faceIndex: Int): ScanFaceDraft {
        val sampledCells = sampleCells(bitmap)
        return ScanFaceDraft(
            faceIndex = faceIndex,
            stickers = ScanColorClassifier.classifyCells(sampledCells),
            capturedAt = clockProvider.now(),
        )
    }

    private fun sampleCells(bitmap: Bitmap): List<RgbColor> {
        OpenCVLoader.initLocal()

        val rgbaMat = Mat()
        Utils.bitmapToMat(bitmap, rgbaMat)

        val rgbMat = Mat()
        Imgproc.cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        Imgproc.GaussianBlur(rgbMat, rgbMat, Size(BLUR_KERNEL, BLUR_KERNEL), 0.0)

        val hsvMat = Mat()
        Imgproc.cvtColor(rgbMat, hsvMat, Imgproc.COLOR_RGB2HSV)

        val squareSize = minOf(bitmap.width, bitmap.height) * GRID_SIZE_RATIO
        val startX = (bitmap.width - squareSize) / 2.0
        val startY = (bitmap.height - squareSize) / 2.0
        val cellSize = squareSize / GRID_SIZE
        val inset = cellSize * sampleInset

        return (0 until GRID_SIZE).flatMap { row ->
            (0 until GRID_SIZE).map { col ->
                val l = (startX + col * cellSize + inset).toInt().coerceAtLeast(0)
                val t = (startY + row * cellSize + inset).toInt().coerceAtLeast(0)
                val r = (startX + (col + 1) * cellSize - inset).toInt().coerceAtMost(bitmap.width)
                val b = (startY + (row + 1) * cellSize - inset).toInt().coerceAtMost(bitmap.height)
                val roi = hsvMat.submat(t, b, l, r)
                val mean = Core.mean(roi)
                hsvToRgb(mean.`val`[0].toFloat(), mean.`val`[1].toFloat(), mean.`val`[2].toFloat())
            }
        }
    }

    // OpenCV HSV scale: H=0-180 (half degrees), S=0-255, V=0-255.
    // Convert to standard H=0-360, S=0-1, V=0-1 before HSV→RGB.
    private fun hsvToRgb(hOcv: Float, sOcv: Float, vOcv: Float): RgbColor {
        val h = hOcv * 2f
        val s = sOcv / 255f
        val v = vOcv / 255f
        if (s == 0f) {
            val gray = (v * 255f).roundToInt().coerceIn(0, 255)
            return RgbColor(gray, gray, gray)
        }
        val sector = h / 60f
        val i = sector.toInt() % 6
        val f = sector - sector.toInt()
        val p = v * (1f - s)
        val q = v * (1f - f * s)
        val t = v * (1f - (1f - f) * s)
        val (r, g, b) = when (i) {
            0    -> Triple(v, t, p)
            1    -> Triple(q, v, p)
            2    -> Triple(p, v, t)
            3    -> Triple(p, q, v)
            4    -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return RgbColor(
            red   = (r * 255f).roundToInt().coerceIn(0, 255),
            green = (g * 255f).roundToInt().coerceIn(0, 255),
            blue  = (b * 255f).roundToInt().coerceIn(0, 255),
        )
    }

    private fun ScanFaceletAssembly.toAppResult(): AppResult<Unit> =
        if (isValid) AppResult.success(Unit)
        else AppResult.failure(AppError.Validation(errorMessage))

    private companion object {
        const val GRID_SIZE = 3
        const val GRID_SIZE_RATIO = 0.82f
        const val BLUR_KERNEL = 7.0
        const val INSET_STICKER = 0.28f
        const val INSET_CARBON  = 0.20f
        const val INSET_FILLED  = 0.12f
    }
}
