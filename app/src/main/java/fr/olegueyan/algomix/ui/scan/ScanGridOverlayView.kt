package fr.olegueyan.algomix.ui.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ScanGridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 210
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height) * GRID_SIZE_RATIO
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val right = left + size
        val bottom = top + size
        canvas.drawRect(left, top, right, bottom, borderPaint)
        val step = size / GRID_LINE_COUNT
        for (index in 1 until GRID_LINE_COUNT) {
            val position = index * step
            canvas.drawLine(left + position, top, left + position, bottom, linePaint)
            canvas.drawLine(left, top + position, right, top + position, linePaint)
        }
    }

    private companion object {
        const val GRID_LINE_COUNT = 3
        const val GRID_SIZE_RATIO = 0.82f
    }
}
