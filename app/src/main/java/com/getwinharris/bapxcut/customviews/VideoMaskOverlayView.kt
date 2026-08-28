package com.getwinharris.bapxcut.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.getwinharris.bapxcut.models.EditOperation

class VideoMaskOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var maskConfig = EditOperation.MaskConfig()
        set(value) {
            field = value
            invalidate()
            onMaskChanged?.invoke(value)
        }

    var isEditingMode = false
        set(value) {
            field = value
            visibility = if (value) VISIBLE else GONE
            invalidate()
        }

    var onMaskChanged: ((EditOperation.MaskConfig) -> Unit)? = null

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6463D7")
        style = Paint.Style.FILL
    }
    
    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var isDragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            maskConfig = maskConfig.copy(
                relativeWidth = (maskConfig.relativeWidth * scaleFactor).coerceIn(0.01f, 5.0f),
                relativeHeight = (maskConfig.relativeHeight * scaleFactor).coerceIn(0.01f, 5.0f)
            )
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditingMode) return false

        scaleDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress || event.pointerCount > 1) {
            isDragging = false
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                dragOffsetX = event.x
                dragOffsetY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - dragOffsetX
                    val dy = event.y - dragOffsetY
                    dragOffsetX = event.x
                    dragOffsetY = event.y
                    
                    if (width > 0 && height > 0) {
                        maskConfig = maskConfig.copy(
                            relativeX = maskConfig.relativeX + dx / width,
                            relativeY = maskConfig.relativeY + dy / height
                        )
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isEditingMode || maskConfig.shape == EditOperation.MaskShape.NONE) return

        canvas.save()
        val cx = width * maskConfig.relativeX
        val cy = height * maskConfig.relativeY
        val mw = width * maskConfig.relativeWidth
        val mh = height * maskConfig.relativeHeight
        
        canvas.rotate(maskConfig.rotationAngle, cx, cy)

        when (maskConfig.shape) {
            EditOperation.MaskShape.RECTANGLE -> canvas.drawRect(cx - mw/2, cy - mh/2, cx + mw/2, cy + mh/2, maskPaint)
            EditOperation.MaskShape.ELLIPSE -> canvas.drawOval(cx - mw/2, cy - mh/2, cx + mw/2, cy + mh/2, maskPaint)
            EditOperation.MaskShape.SPLIT -> canvas.drawLine(-width.toFloat(), cy, width * 2f, cy, maskPaint)
            EditOperation.MaskShape.SHUTTER -> {
                canvas.drawLine(-width.toFloat(), cy - mh/2, width * 2f, cy - mh/2, maskPaint)
                canvas.drawLine(-width.toFloat(), cy + mh/2, width * 2f, cy + mh/2, maskPaint)
            }
            EditOperation.MaskShape.HEART -> {
                val path = android.graphics.Path()
                createHeartPath(path, cx, cy, mw, mh)
                canvas.drawPath(path, maskPaint)
            }
            EditOperation.MaskShape.STAR -> {
                val path = android.graphics.Path()
                createStarPath(path, cx, cy, mw / 2f, mw / 4f)
                canvas.drawPath(path, maskPaint)
            }
            else -> {}
        }

        // Draw mask center handle
        canvas.drawCircle(cx, cy, 12f, handleFillPaint)
        canvas.drawCircle(cx, cy, 12f, handleStrokePaint)

        canvas.restore()
    }

    private fun createHeartPath(path: android.graphics.Path, cx: Float, cy: Float, width: Float, height: Float) {
        path.reset()
        val topCurveHeight = height * 0.3f
        path.moveTo(cx, cy + height * 0.4f)
        path.cubicTo(
            cx - width * 0.5f, cy + height * 0.1f,
            cx - width * 0.5f, cy - topCurveHeight,
            cx, cy - topCurveHeight * 0.4f
        )
        path.cubicTo(
            cx + width * 0.5f, cy - topCurveHeight,
            cx + width * 0.5f, cy + height * 0.1f,
            cx, cy + height * 0.4f
        )
        path.close()
    }

    private fun createStarPath(path: android.graphics.Path, cx: Float, cy: Float, radiusOuter: Float, radiusInner: Float) {
        path.reset()
        val points = 5
        val angle = Math.PI / points
        for (i in 0 until 2 * points) {
            val r = if (i % 2 == 0) radiusOuter else radiusInner
            val currAngle = i * angle - Math.PI / 2
            val x = (cx + r * Math.cos(currAngle)).toFloat()
            val y = (cy + r * Math.sin(currAngle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}
