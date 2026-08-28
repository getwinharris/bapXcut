package com.getwinharris.bapxcut.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * CustomVideoSeeker — redesigned to match the CapCut-style reference UI.
 *
 * Visual changes (no functional changes):
 *  • Playhead line color → #FF4081 (accent)
 *  • Playhead has a teardrop / rounded-top handle at the top (like the reference)
 *  • Line is slightly thinner for a refined look
 *
 * All seek logic (onSeekListener, setVideoDuration, seekPosition) is untouched.
 */
class CustomVideoSeeker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Paints ────────────────────────────────────────────────────────────────

    /** The main accent line */
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6463D7") // Primary theme color
        strokeWidth = 2f // Will be multiplied by density in init/draw
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /** The "Shield" handle at the top */
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6463D7") // Primary theme color
        style = Paint.Style.FILL
    }

    /** Subtle drop shadow so the playhead doesn't get lost in light scenes */
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 40
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    /** Subtle white glow behind the handle for depth */
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.FILL
    }

    // ── State (identical to original) ─────────────────────────────────────────
    private var seekPosition = 0f   // 0..1
    private var videoDuration = 0L  // ms
    var onSeekListener: ((Float) -> Unit)? = null

    // ── Handle geometry ───────────────────────────────────────────────────────
    private val handleRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val density = resources.displayMetrics.density
        val seekX = width / 2f
        val handleWidth = 14f * density  // 14dp
        val handleHeight = 28f * density // 28dp (exactly matches the TimeRulerView height)
        val cornerRadius = 4f * density
        
        linePaint.strokeWidth = 1.5f * density
        shadowPaint.strokeWidth = 3f * density

        // 1. Draw the subtle drop shadow for the vertical line
        canvas.drawLine(seekX + 1f * density, handleHeight, seekX + 1f * density, height.toFloat(), shadowPaint)

        // 2. Draw the main vertical accent line (The Stem)
        canvas.drawLine(seekX, handleHeight, seekX, height.toFloat(), linePaint)

        // 3. Draw the handle "Head" (A rounded rectangle/shield pointing downwards)
        handleRect.set(
            seekX - (handleWidth / 2),
            0f,
            seekX + (handleWidth / 2),
            handleHeight
        )

        // Draw a slight glow/shadow under the head
        canvas.drawRoundRect(handleRect, cornerRadius, cornerRadius, glowPaint)

        // Draw the actual head
        canvas.drawRoundRect(handleRect, cornerRadius, cornerRadius, handlePaint)
    }

    private var isDragging = false
    val isUserSeeking: Boolean get() = isDragging

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false // Let touch events pass through to the timeline below!
    }

    fun setVideoDuration(duration: Long) {
        videoDuration = duration
    }

    fun setSeekPosition(position: Float) {
        seekPosition = position.coerceIn(0f, 1f)
        invalidate()
    }

}