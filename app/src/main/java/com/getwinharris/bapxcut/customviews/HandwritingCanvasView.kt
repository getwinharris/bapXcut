package com.getwinharris.bapxcut.customviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Interactive drawing overlay view for handwriting and scribbling on video frames.
 * Supports smooth quadratic curve pathing, undo/redo, dynamic brush sizing, custom colors,
 * and relative coordinate transparent PNG bitmap export.
 */
class HandwritingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Stroke(
        val path: Path,
        val color: Int,
        val strokeWidthPx: Float
    )

    private val strokes = mutableListOf<Stroke>()
    private val redoStrokes = mutableListOf<Stroke>()

    private var currentPath: Path? = null
    private var currentX = 0f
    private var currentY = 0f

    var activeColor: Int = Color.parseColor("#FF007A")
        private set
    var activeStrokeWidthPx: Float = 12f * resources.displayMetrics.density
        private set

    private var videoRect: RectF? = null

    private val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    var onStrokesChangedListener: (() -> Unit)? = null

    fun setVideoRect(rect: RectF) {
        this.videoRect = RectF(rect)
        invalidate()
    }

    fun setBrushColor(color: Int) {
        this.activeColor = color
        invalidate()
    }

    fun setBrushSizePx(sizePx: Float) {
        this.activeStrokeWidthPx = sizePx
        invalidate()
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            val removed = strokes.removeAt(strokes.size - 1)
            redoStrokes.add(removed)
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    fun redo() {
        if (redoStrokes.isNotEmpty()) {
            val restored = redoStrokes.removeAt(redoStrokes.size - 1)
            strokes.add(restored)
            invalidate()
            onStrokesChangedListener?.invoke()
        }
    }

    fun clear() {
        strokes.clear()
        redoStrokes.clear()
        currentPath = null
        invalidate()
        onStrokesChangedListener?.invoke()
    }

    fun canUndo(): Boolean = strokes.isNotEmpty()
    fun canRedo(): Boolean = redoStrokes.isNotEmpty()
    fun isEmpty(): Boolean = strokes.isEmpty() && currentPath == null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        val x = event.x
        val y = event.y

        // Check if inside video rect bounds if set
        val vRect = videoRect ?: RectF(0f, 0f, width.toFloat(), height.toFloat())

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val clampedX = x.coerceIn(vRect.left, vRect.right)
                val clampedY = y.coerceIn(vRect.top, vRect.bottom)
                currentPath = Path().apply {
                    moveTo(clampedX, clampedY)
                }
                currentX = clampedX
                currentY = clampedY
                redoStrokes.clear()
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val clampedX = x.coerceIn(vRect.left, vRect.right)
                val clampedY = y.coerceIn(vRect.top, vRect.bottom)
                val dx = Math.abs(clampedX - currentX)
                val dy = Math.abs(clampedY - currentY)
                if (dx >= 3f || dy >= 3f) {
                    currentPath?.quadTo(currentX, currentY, (clampedX + currentX) / 2f, (clampedY + currentY) / 2f)
                    currentX = clampedX
                    currentY = clampedY
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val clampedX = x.coerceIn(vRect.left, vRect.right)
                val clampedY = y.coerceIn(vRect.top, vRect.bottom)
                currentPath?.lineTo(clampedX, clampedY)
                currentPath?.let { path ->
                    strokes.add(Stroke(path, activeColor, activeStrokeWidthPx))
                }
                currentPath = null
                invalidate()
                onStrokesChangedListener?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw existing strokes
        for (stroke in strokes) {
            currentPaint.color = stroke.color
            currentPaint.strokeWidth = stroke.strokeWidthPx
            canvas.drawPath(stroke.path, currentPaint)
        }

        // Draw currently active stroke in real-time
        currentPath?.let { path ->
            currentPaint.color = activeColor
            currentPaint.strokeWidth = activeStrokeWidthPx
            canvas.drawPath(path, currentPaint)
        }
    }

    /**
     * Renders all handwriting strokes onto a transparent ARGB_8888 Bitmap scaled to the
     * requested target width and height (e.g. video export resolution 1920x1080).
     */
    fun exportToBitmap(targetWidth: Int, targetHeight: Int): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null
        val vRect = videoRect ?: RectF(0f, 0f, width.toFloat(), height.toFloat())
        if (vRect.width() <= 0f || vRect.height() <= 0f) return null

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        // Scale factors mapping videoRect coordinates to target image dimensions
        val scaleX = targetWidth.toFloat() / vRect.width()
        val scaleY = targetHeight.toFloat() / vRect.height()
        val avgScale = (scaleX + scaleY) / 2f

        val matrix = Matrix().apply {
            postTranslate(-vRect.left, -vRect.top)
            postScale(scaleX, scaleY)
        }

        val renderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val tempPath = Path()
        for (stroke in strokes) {
            tempPath.reset()
            stroke.path.transform(matrix, tempPath)
            renderPaint.color = stroke.color
            renderPaint.strokeWidth = stroke.strokeWidthPx * avgScale
            canvas.drawPath(tempPath, renderPaint)
        }

        return bitmap
    }
}
