package com.getwinharris.bapxcut.customviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TransitionPreviewOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var transitionType: String = "none"
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private var snapshotBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val srcRect = Rect()
    private val dstRect = RectF()

    fun updateTransition(type: String, prog: Float, bitmap: Bitmap?) {
        this.transitionType = type.lowercase()
        this.progress = prog.coerceIn(0f, 1f)
        if (this.snapshotBitmap != bitmap) {
            this.snapshotBitmap = bitmap
        }
        invalidate()
    }

    fun clearSnapshot() {
        this.snapshotBitmap = null
        this.transitionType = "none"
        this.progress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bmp = snapshotBitmap
        if (bmp == null || bmp.isRecycled || transitionType == "none" || progress <= 0f || progress >= 1f) {
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        srcRect.set(0, 0, bmp.width, bmp.height)
        dstRect.set(0f, 0f, w, h)

        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true

        when (transitionType) {
            "fade", "dissolve" -> {
                paint.alpha = ((1f - progress) * 255).toInt()
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
            }
            "fadeblack" -> {
                val alpha = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                paint.color = Color.BLACK
                paint.alpha = (alpha * 255).toInt()
                canvas.drawRect(dstRect, paint)
            }
            "fadewhite" -> {
                val alpha = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                paint.color = Color.WHITE
                paint.alpha = (alpha * 255).toInt()
                canvas.drawRect(dstRect, paint)
            }
            "wipeleft" -> {
                canvas.save()
                canvas.clipRect(0f, 0f, w * (1f - progress), h)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "wiperight" -> {
                canvas.save()
                canvas.clipRect(w * progress, 0f, w, h)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "wipeup" -> {
                canvas.save()
                canvas.clipRect(0f, 0f, w, h * (1f - progress))
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "wipedown" -> {
                canvas.save()
                canvas.clipRect(0f, h * progress, w, h)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "slideleft", "coverleft" -> {
                canvas.save()
                canvas.translate(-w * progress, 0f)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "slideright", "coverright" -> {
                canvas.save()
                canvas.translate(w * progress, 0f)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "slideup" -> {
                canvas.save()
                canvas.translate(0f, -h * progress)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "slidedown" -> {
                canvas.save()
                canvas.translate(0f, h * progress)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "circlecrop" -> {
                canvas.save()
                val maxRadius = Math.hypot((w / 2).toDouble(), (h / 2).toDouble()).toFloat()
                val radius = maxRadius * (1f - progress)
                path.reset()
                path.addCircle(w / 2f, h / 2f, radius, Path.Direction.CW)
                canvas.clipPath(path)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "rectcrop" -> {
                canvas.save()
                val halfW = (w / 2f) * (1f - progress)
                val halfH = (h / 2f) * (1f - progress)
                canvas.clipRect(w / 2f - halfW, h / 2f - halfH, w / 2f + halfW, h / 2f + halfH)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            "zoomin" -> {
                canvas.save()
                val scale = 1f + progress * 0.4f
                paint.alpha = ((1f - progress) * 255).toInt()
                canvas.scale(scale, scale, w / 2f, h / 2f)
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                canvas.restore()
            }
            else -> {
                paint.alpha = ((1f - progress) * 255).toInt()
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
            }
        }
    }
}
