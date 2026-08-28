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

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var videoWidth = 0
    private var videoHeight = 0

    // Crop box fractions (normalized relative to the video rectangle width & height)
    private var leftFrac = 0.1f
    private var topFrac = 0.1f
    private var rightFrac = 0.9f
    private var bottomFrac = 0.9f

    private val handleRadius = 10f * resources.displayMetrics.density
    private val touchTarget = 30f * resources.displayMetrics.density

    private enum class TouchState {
        NONE, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, CENTER
    }

    private var touchState = TouchState.NONE
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialLeftFrac = 0.1f
    private var initialTopFrac = 0.1f
    private var initialRightFrac = 0.9f
    private var initialBottomFrac = 0.9f

    var onCropBoundsChanged: ((xFraction: Float, yFraction: Float, wFraction: Float, hFraction: Float) -> Unit)? = null

    fun setVideoSize(width: Int, height: Int) {
        this.videoWidth = width
        this.videoHeight = height
        invalidate()
    }

    fun setCropBounds(x: Float, y: Float, w: Float, h: Float) {
        leftFrac = x.coerceIn(0f, 1f)
        topFrac = y.coerceIn(0f, 1f)
        rightFrac = (x + w).coerceIn(0f, 1f)
        bottomFrac = (y + h).coerceIn(0f, 1f)
        invalidate()
    }

    fun getCropBounds(): FloatArray {
        return floatArrayOf(leftFrac, topFrac, rightFrac - leftFrac, bottomFrac - topFrac)
    }

    private fun getVideoRect(): RectF {
        val rect = RectF()
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            return rect
        }

        val containerRatio = width.toFloat() / height
        val videoRatio = videoWidth.toFloat() / videoHeight

        if (videoRatio > containerRatio) {
            val h = width / videoRatio
            val top = (height - h) / 2f
            rect.set(0f, top, width.toFloat(), top + h)
        } else {
            val w = height * videoRatio
            val left = (width - w) / 2f
            rect.set(left, 0f, left + w, height.toFloat())
        }
        return rect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (videoWidth <= 0 || videoHeight <= 0) return

        val videoRect = getVideoRect()
        val cropLeft = videoRect.left + leftFrac * videoRect.width()
        val cropTop = videoRect.top + topFrac * videoRect.height()
        val cropRight = videoRect.left + rightFrac * videoRect.width()
        val cropBottom = videoRect.top + bottomFrac * videoRect.height()

        // 1. Draw dim overlays outside the crop box
        // Top
        canvas.drawRect(videoRect.left, videoRect.top, videoRect.right, cropTop, overlayPaint)
        // Left
        canvas.drawRect(videoRect.left, cropTop, cropLeft, cropBottom, overlayPaint)
        // Right
        canvas.drawRect(cropRight, cropTop, videoRect.right, cropBottom, overlayPaint)
        // Bottom
        canvas.drawRect(videoRect.left, cropBottom, videoRect.right, videoRect.bottom, overlayPaint)

        // 2. Draw border
        canvas.drawRect(cropLeft, cropTop, cropRight, cropBottom, borderPaint)

        // 3. Draw Rule of Thirds grid lines
        val cropW = cropRight - cropLeft
        val cropH = cropBottom - cropTop

        val x1 = cropLeft + cropW / 3f
        val x2 = cropLeft + 2f * cropW / 3f
        canvas.drawLine(x1, cropTop, x1, cropBottom, gridPaint)
        canvas.drawLine(x2, cropTop, x2, cropBottom, gridPaint)

        val y1 = cropTop + cropH / 3f
        val y2 = cropTop + 2f * cropH / 3f
        canvas.drawLine(cropLeft, y1, cropRight, y1, gridPaint)
        canvas.drawLine(cropLeft, y2, cropRight, y2, gridPaint)

        // 4. Draw corner handles
        canvas.drawCircle(cropLeft, cropTop, handleRadius, handlePaint)
        canvas.drawCircle(cropRight, cropTop, handleRadius, handlePaint)
        canvas.drawCircle(cropLeft, cropBottom, handleRadius, handlePaint)
        canvas.drawCircle(cropRight, cropBottom, handleRadius, handlePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val videoRect = getVideoRect()
        val cropLeft = videoRect.left + leftFrac * videoRect.width()
        val cropTop = videoRect.top + topFrac * videoRect.height()
        val cropRight = videoRect.left + rightFrac * videoRect.width()
        val cropBottom = videoRect.top + bottomFrac * videoRect.height()

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Determine touch target
                touchState = when {
                    dist(touchX, touchY, cropLeft, cropTop) < touchTarget -> TouchState.LEFT_TOP
                    dist(touchX, touchY, cropRight, cropTop) < touchTarget -> TouchState.RIGHT_TOP
                    dist(touchX, touchY, cropLeft, cropBottom) < touchTarget -> TouchState.LEFT_BOTTOM
                    dist(touchX, touchY, cropRight, cropBottom) < touchTarget -> TouchState.RIGHT_BOTTOM
                    touchX in cropLeft..cropRight && touchY in cropTop..cropBottom -> TouchState.CENTER
                    else -> TouchState.NONE
                }

                if (touchState != TouchState.NONE) {
                    dragStartX = touchX
                    dragStartY = touchY
                    initialLeftFrac = leftFrac
                    initialTopFrac = topFrac
                    initialRightFrac = rightFrac
                    initialBottomFrac = bottomFrac
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchState == TouchState.NONE) return false

                val dx = (touchX - dragStartX) / videoRect.width()
                val dy = (touchY - dragStartY) / videoRect.height()

                val minSizeFrac = 50f / maxOf(1f, maxOf(videoRect.width(), videoRect.height()))

                when (touchState) {
                    TouchState.LEFT_TOP -> {
                        leftFrac = (initialLeftFrac + dx).coerceIn(0f, rightFrac - minSizeFrac)
                        topFrac = (initialTopFrac + dy).coerceIn(0f, bottomFrac - minSizeFrac)
                    }
                    TouchState.RIGHT_TOP -> {
                        rightFrac = (initialRightFrac + dx).coerceIn(leftFrac + minSizeFrac, 1f)
                        topFrac = (initialTopFrac + dy).coerceIn(0f, bottomFrac - minSizeFrac)
                    }
                    TouchState.LEFT_BOTTOM -> {
                        leftFrac = (initialLeftFrac + dx).coerceIn(0f, rightFrac - minSizeFrac)
                        bottomFrac = (initialBottomFrac + dy).coerceIn(topFrac + minSizeFrac, 1f)
                    }
                    TouchState.RIGHT_BOTTOM -> {
                        rightFrac = (initialRightFrac + dx).coerceIn(leftFrac + minSizeFrac, 1f)
                        bottomFrac = (initialBottomFrac + dy).coerceIn(topFrac + minSizeFrac, 1f)
                    }
                    TouchState.CENTER -> {
                        val w = initialRightFrac - initialLeftFrac
                        val h = initialBottomFrac - initialTopFrac

                        leftFrac = (initialLeftFrac + dx).coerceIn(0f, 1f - w)
                        topFrac = (initialTopFrac + dy).coerceIn(0f, 1f - h)
                        rightFrac = leftFrac + w
                        bottomFrac = topFrac + h
                    }
                    else -> {}
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchState != TouchState.NONE) {
                    touchState = TouchState.NONE
                    onCropBoundsChanged?.invoke(leftFrac, topFrac, rightFrac - leftFrac, bottomFrac - topFrac)
                    return true
                }
            }
        }
        return false
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
