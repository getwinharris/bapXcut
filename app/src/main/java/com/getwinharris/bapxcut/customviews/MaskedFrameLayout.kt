package com.getwinharris.bapxcut.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Region
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import com.getwinharris.bapxcut.models.EditOperation

class MaskedFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var maskConfig: EditOperation.MaskConfig = EditOperation.MaskConfig()
        set(value) {
            field = value
            invalidate()
        }

    override fun dispatchDraw(canvas: Canvas) {
        if (maskConfig.shape != EditOperation.MaskShape.NONE) {
            val path = Path()
            val cx = width * maskConfig.relativeX
            val cy = height * maskConfig.relativeY
            val mw = width * maskConfig.relativeWidth
            val mh = height * maskConfig.relativeHeight

            when (maskConfig.shape) {
                EditOperation.MaskShape.RECTANGLE -> path.addRect(cx - mw/2, cy - mh/2, cx + mw/2, cy + mh/2, Path.Direction.CW)
                EditOperation.MaskShape.ELLIPSE -> path.addOval(cx - mw/2, cy - mh/2, cx + mw/2, cy + mh/2, Path.Direction.CW)
                EditOperation.MaskShape.SPLIT -> path.addRect(-width.toFloat(), cy, width * 2f, height * 2f, Path.Direction.CW)
                EditOperation.MaskShape.SHUTTER -> path.addRect(-width.toFloat(), cy - mh/2, width * 2f, cy + mh/2, Path.Direction.CW)
                EditOperation.MaskShape.HEART -> createHeartPath(path, cx, cy, mw, mh)
                EditOperation.MaskShape.STAR -> createStarPath(path, cx, cy, mw / 2f, mw / 4f)
                else -> {}
            }

            if (maskConfig.rotationAngle != 0f) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(maskConfig.rotationAngle, cx, cy)
                path.transform(matrix)
            }

            if (maskConfig.feather > 0f) {
                val featherPx = (maskConfig.feather * 0.4f).coerceIn(1f, 80f)
                val count = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
                super.dispatchDraw(canvas)
                
                val maskPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = -0x1
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                    maskFilter = android.graphics.BlurMaskFilter(featherPx, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
                
                if (maskConfig.isInverted) {
                    val fullPath = Path().apply { addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        fullPath.op(path, Path.Op.DIFFERENCE)
                    }
                    canvas.drawPath(fullPath, maskPaint)
                } else {
                    canvas.drawPath(path, maskPaint)
                }
                canvas.restoreToCount(count)
            } else {
                canvas.save()
                if (maskConfig.isInverted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        canvas.clipOutPath(path)
                    } else {
                        canvas.clipPath(path, Region.Op.DIFFERENCE)
                    }
                } else {
                    canvas.clipPath(path)
                }
                super.dispatchDraw(canvas)
                canvas.restore()
            }
        } else {
            super.dispatchDraw(canvas)
        }
    }

    private fun createHeartPath(path: Path, cx: Float, cy: Float, width: Float, height: Float) {
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

    private fun createStarPath(path: Path, cx: Float, cy: Float, radiusOuter: Float, radiusInner: Float) {
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
