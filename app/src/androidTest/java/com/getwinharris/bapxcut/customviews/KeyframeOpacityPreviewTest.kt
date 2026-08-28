package com.getwinharris.bapxcut.customviews

import android.graphics.Color
import android.widget.EditText
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyframeOpacityPreviewTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun setPropertiesAppliesMidpointOpacityToImageAndTextPreviews() {
        instrumentation.runOnMainSync {
            val imageOverlay = DraggableImageOverlayView(context)
            val image = imageOverlay.getChildAt(0) as ImageView
            imageOverlay.setProperties(0.5f, 0.5f, 0.3f, 0.3f, 0f, 0.5f, false)

            assertEquals(0.5f, imageOverlay.getOpacity(), FLOAT_DELTA)
            assertEquals(0.5f, image.alpha, FLOAT_DELTA)

            val textOverlay = DraggableTextOverlayView(context)
            val text = textOverlay.getChildAt(0) as EditText
            textOverlay.setTextColor("#336699")
            textOverlay.setProperties(0.5f, 0.5f, 0.5f)

            assertEquals(0.5f, textOverlay.getOpacity(), FLOAT_DELTA)
            assertEquals(127, Color.alpha(text.currentTextColor))
            assertEquals(0x33, Color.red(text.currentTextColor))
            assertEquals(0x66, Color.green(text.currentTextColor))
            assertEquals(0x99, Color.blue(text.currentTextColor))
        }
    }

    @Test
    fun setPropertiesAppliesFullyTransparentAndOpaqueEndpoints() {
        instrumentation.runOnMainSync {
            val imageOverlay = DraggableImageOverlayView(context)
            val image = imageOverlay.getChildAt(0) as ImageView
            val textOverlay = DraggableTextOverlayView(context)
            val text = textOverlay.getChildAt(0) as EditText

            imageOverlay.setProperties(0.5f, 0.5f, 0.3f, 0.3f, 0f, 0f, false)
            textOverlay.setProperties(0.5f, 0.5f, 0f)

            assertEquals(0f, image.alpha, FLOAT_DELTA)
            assertEquals(0, Color.alpha(text.currentTextColor))

            imageOverlay.setProperties(0.5f, 0.5f, 0.3f, 0.3f, 0f, 1f, false)
            textOverlay.setProperties(0.5f, 0.5f, 1f)

            assertEquals(1f, image.alpha, FLOAT_DELTA)
            assertEquals(255, Color.alpha(text.currentTextColor))
        }
    }

    @Test
    fun repeatedPositionUpdatesKeepSuppliedOpacityVisible() {
        instrumentation.runOnMainSync {
            val imageOverlay = DraggableImageOverlayView(context)
            val image = imageOverlay.getChildAt(0) as ImageView
            val textOverlay = DraggableTextOverlayView(context)
            val text = textOverlay.getChildAt(0) as EditText

            imageOverlay.setProperties(0.25f, 0.25f, 0.3f, 0.3f, 0f, 0.4f, false)
            textOverlay.setProperties(0.25f, 0.25f, 0.4f)
            imageOverlay.setProperties(0.75f, 0.75f, 0.3f, 0.3f, 0f, 0.4f, false)
            textOverlay.setProperties(0.75f, 0.75f, 0.4f)

            assertEquals(0.4f, image.alpha, FLOAT_DELTA)
            assertEquals((0.4f * 255).toInt(), Color.alpha(text.currentTextColor))
        }
    }

    @Test
    fun imageOpacityCanBeAppliedBeforeMediaLoads() {
        instrumentation.runOnMainSync {
            val imageOverlay = DraggableImageOverlayView(context)
            val image = imageOverlay.getChildAt(0) as ImageView

            imageOverlay.setProperties(0.5f, 0.5f, 0.3f, 0.3f, 0f, 0.25f, false)

            assertEquals(0.25f, imageOverlay.getOpacity(), FLOAT_DELTA)
            assertEquals(0.25f, image.alpha, FLOAT_DELTA)
        }
    }

    @Test
    fun invalidTextColorKeepsFallbackWhenOpacityIsApplied() {
        instrumentation.runOnMainSync {
            val textOverlay = DraggableTextOverlayView(context)
            val text = textOverlay.getChildAt(0) as EditText
            textOverlay.setTextColor("not-a-color")

            textOverlay.setProperties(0.5f, 0.5f, 0.5f)

            assertEquals(0.5f, textOverlay.getOpacity(), FLOAT_DELTA)
            assertEquals(Color.WHITE, text.currentTextColor)
        }
    }

    private companion object {
        const val FLOAT_DELTA = 0.0001f
    }
}
