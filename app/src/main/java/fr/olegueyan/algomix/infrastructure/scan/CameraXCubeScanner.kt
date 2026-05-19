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

class CameraXCubeScanner(
    private val clockProvider: ClockProvider = SystemClockProvider,
) : CubeScanner {
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

    fun extractFaceFromBitmap(
        bitmap: Bitmap,
        faceIndex: Int,
    ): ScanFaceDraft {
        val sampledCells = sampleCells(bitmap)
        return ScanFaceDraft(
            faceIndex = faceIndex,
            stickers = ScanColorClassifier.classifyCells(sampledCells),
            capturedAt = clockProvider.now(),
        )
    }

    private fun sampleCells(bitmap: Bitmap): List<RgbColor> {
        val squareSize = minOf(bitmap.width, bitmap.height) * GRID_SIZE_RATIO
        val startX = (bitmap.width - squareSize) / 2
        val startY = (bitmap.height - squareSize) / 2
        val cellSize = squareSize / GRID_SIZE.toFloat()
        return (0 until GRID_SIZE).flatMap { row ->
            (0 until GRID_SIZE).map { column ->
                bitmap.averageColor(
                    left = (startX + column * cellSize + cellSize * SAMPLE_INSET).toNearestPixel(),
                    top = (startY + row * cellSize + cellSize * SAMPLE_INSET).toNearestPixel(),
                    right = (startX + (column + 1) * cellSize - cellSize * SAMPLE_INSET).toNearestPixel(),
                    bottom = (startY + (row + 1) * cellSize - cellSize * SAMPLE_INSET).toNearestPixel(),
                )
            }
        }
    }

    private fun Bitmap.averageColor(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): RgbColor {
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(height)) {
            for (x in left.coerceAtLeast(0) until right.coerceAtMost(width)) {
                val pixel = getPixel(x, y)
                red += android.graphics.Color.red(pixel)
                green += android.graphics.Color.green(pixel)
                blue += android.graphics.Color.blue(pixel)
                count++
            }
        }
        val safeCount = count.coerceAtLeast(1L)
        return RgbColor(
            red = (red / safeCount).toInt(),
            green = (green / safeCount).toInt(),
            blue = (blue / safeCount).toInt(),
        )
    }

    private fun ScanFaceletAssembly.toAppResult(): AppResult<Unit> =
        if (isValid) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(AppError.Validation(errorMessage))
        }

    private fun Float.toNearestPixel(): Int =
        (this + HALF_PIXEL).toInt()

    private companion object {
        const val GRID_SIZE = 3
        const val GRID_SIZE_RATIO = 0.82f
        const val SAMPLE_INSET = 0.28f
        const val HALF_PIXEL = 0.5f
    }
}
