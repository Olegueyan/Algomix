package fr.olegueyan.algomix.ui.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.scan.ScanFaceDraft
import fr.olegueyan.algomix.domain.scan.ScanSessionDraft
import kotlin.math.min

class ScanCubeProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private var session = ScanSessionDraft()
    private val cell = RectF()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDER_COLOR
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACTIVE_COLOR
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    @Suppress("UNUSED_PARAMETER")
    fun render(session: ScanSessionDraft, currentDraft: ScanFaceDraft?) {
        this.session = session
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val gap = 8f * density
        val faceGap = 10f * density
        val availableFaceWidth = (width - gap - paddingLeft - paddingRight) / COLUMNS
        val availableFaceHeight = (height - faceGap - paddingTop - paddingBottom) / ROWS
        val faceSize = min(availableFaceWidth, availableFaceHeight).coerceAtLeast(0f)
        val totalWidth = faceSize * COLUMNS + gap
        val totalHeight = faceSize * ROWS + faceGap
        val startX = paddingLeft + (width - paddingLeft - paddingRight - totalWidth) / 2f
        val startY = paddingTop + (height - paddingTop - paddingBottom - totalHeight) / 2f

        repeat(FACE_COUNT) { faceIndex ->
            val row = faceIndex / COLUMNS
            val column = faceIndex % COLUMNS
            val left = startX + column * (faceSize + gap)
            val top = startY + row * (faceSize + faceGap)
            drawFace(canvas, faceIndex, left, top, faceSize)
        }
    }

    private fun drawFace(canvas: Canvas, faceIndex: Int, left: Float, top: Float, size: Float) {
        val stickers = session.face(faceIndex)?.takeIf { it.isComplete }?.stickers.orEmpty()
        val stickerSize = size / GRID_SIZE
        repeat(ScanFaceDraft.STICKER_COUNT) { stickerIndex ->
            val row = stickerIndex / GRID_SIZE
            val column = stickerIndex % GRID_SIZE
            cell.set(
                left + column * stickerSize,
                top + row * stickerSize,
                left + (column + 1) * stickerSize,
                top + (row + 1) * stickerSize,
            )
            fillPaint.color = stickers.getOrNull(stickerIndex)?.toAndroidColor() ?: EMPTY_COLOR
            canvas.drawRect(cell, fillPaint)
            canvas.drawRect(cell, borderPaint)
        }
        cell.set(left, top, left + size, top + size)
        canvas.drawRect(cell, borderPaint)
        if (faceIndex == session.currentFaceIndex && !session.isComplete) {
            canvas.drawRect(cell, activePaint)
        }
    }

    private fun FaceColor.toAndroidColor(): Int =
        Color.rgb(
            (r * RGB_MAX).toInt().coerceIn(0, RGB_MAX),
            (g * RGB_MAX).toInt().coerceIn(0, RGB_MAX),
            (b * RGB_MAX).toInt().coerceIn(0, RGB_MAX),
        )

    companion object {
        private const val FACE_COUNT = 6
        private const val COLUMNS = 3
        private const val ROWS = 2
        private const val GRID_SIZE = 3
        private const val RGB_MAX = 255
        private const val EMPTY_COLOR = 0xFF6F737A.toInt()
        private const val BORDER_COLOR = 0xFF050505.toInt()
        private const val ACTIVE_COLOR = 0xFFFF8A00.toInt()
    }
}
