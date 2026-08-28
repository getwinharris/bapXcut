package com.getwinharris.bapxcut.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Locale

class TimeRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var videoDurationMs: Long = 0L

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F3F4A") // outline
        strokeWidth = 2f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A8A93") // toolTextInactive / inactiveTool
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A8A93")
        textSize = 24f // approx 10sp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    init {
        val density = resources.displayMetrics.density
        textPaint.textSize = 9f * density // 9sp for clean look
    }

    fun setVideoDuration(durationMs: Long) {
        this.videoDurationMs = durationMs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (videoDurationMs <= 0 || width <= 0) return

        val msPerPixel = videoDurationMs.toFloat() / width
        if (msPerPixel <= 0f) return

        val density = resources.displayMetrics.density
        
        // Dynamic step calculations based on pixels/zoom level
        // We target roughly 80dp of space between major ticks
        val targetWidthPx = 80f * density
        val targetMs = targetWidthPx * msPerPixel

        val stepMs = when {
            targetMs <= 100L -> 100L
            targetMs <= 200L -> 200L
            targetMs <= 500L -> 500L
            targetMs <= 1000L -> 1000L
            targetMs <= 2000L -> 2000L
            targetMs <= 5000L -> 5000L
            targetMs <= 10000L -> 10000L
            targetMs <= 30000L -> 30000L
            targetMs <= 60000L -> 60000L
            targetMs <= 120000L -> 120000L
            targetMs <= 300000L -> 300000L
            else -> 600000L
        }

        // Minor step is always 1/5th of major step
        val minorStepMs = stepMs / 5

        val heightVal = height.toFloat()
        val textY = 12f * density // Draw text near the top
        val tickBottom = heightVal - 2f * density

        // Draw minor and major ticks
        var currentMs = 0L
        while (currentMs <= videoDurationMs) {
            val x = currentMs / msPerPixel

            if (currentMs % stepMs == 0L) {
                // Major tick: clean line from middle to bottom
                canvas.drawLine(x, heightVal * 0.5f, x, tickBottom, majorTickPaint)

                // Format time string (e.g., 00:02 or 01:30 or 00:02.5 for sub-seconds)
                val minutes = (currentMs / 60000).toInt()
                val seconds = ((currentMs % 60000) / 1000).toInt()
                val timeStr = if (stepMs < 1000L) {
                    val tenths = ((currentMs % 1000) / 100).toInt()
                    String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                // Draw label at the top center of tick
                canvas.drawText(timeStr, x, textY, textPaint)
            } else {
                // Minor tick: short thin line near bottom
                canvas.drawLine(x, heightVal * 0.75f, x, tickBottom, tickPaint)
            }

            currentMs += minorStepMs
        }
    }
}
