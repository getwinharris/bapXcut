package com.getwinharris.bapxcut.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom view that displays a dynamic circle dot representing brush stroke size and color.
 * Outlined with a subtle border ring to ensure visibility across dark/light UI modes.
 */
class BrushSizeDotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var brushSizePx: Float = 12f
    private var minBrushPx: Float = 2f
    private var maxBrushPx: Float = 100f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF007A") // Slate Blue default
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(180, 255, 255, 255)
    }

    fun setBrushSize(sizePx: Float, minPx: Float = 2f, maxPx: Float = 100f) {
        this.brushSizePx = sizePx
        this.minBrushPx = minPx
        this.maxBrushPx = maxPx
        invalidate()
    }

    fun setBrushColor(color: Int) {
        fillPaint.color = color
        // Adjust border contrast based on luminance if needed
        val alpha = Color.alpha(color)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        borderPaint.color = if (luminance > 0.8 && alpha > 128) {
            Color.parseColor("#44000000")
        } else {
            Color.parseColor("#88FFFFFF")
        }
        invalidate()
    }

    fun getBrushColor(): Int = fillPaint.color

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxAvailableRadius = (minOf(width, height) / 2f) - 6f
        if (maxAvailableRadius <= 0) return

        // Scale radius visually: map [minBrushPx..maxBrushPx] -> [4dp..maxAvailableRadius]
        val range = (maxBrushPx - minBrushPx).coerceAtLeast(1f)
        val fraction = ((brushSizePx - minBrushPx) / range).coerceIn(0f, 1f)
        val minRadius = 4f * resources.displayMetrics.density
        val radius = (minRadius + fraction * (maxAvailableRadius - minRadius)).coerceAtMost(maxAvailableRadius)

        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, borderPaint)
    }
}
