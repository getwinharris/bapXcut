package com.getwinharris.bapxcut

import android.app.Application
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BrandingTest {
    @Test
    fun onboardingUsesForkBrandAndTheme() {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_welcome_onboarding, null)
        assertEquals("bapXcut", view.findViewById<TextView>(R.id.tvOnboardingTitle).text.toString())
        assertEquals("com.getwinharris.bapxcut", context.packageName)
        assertEquals(Color.parseColor("#6463D7"), context.getColor(R.color.colorPrimary))
        assertEquals(Color.BLACK, context.getColor(R.color.background))
    }

    @Test
    @Config(qualifiers = "de")
    fun translatedOnboardingKeepsForkName() {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_welcome_onboarding, null)
        assertEquals("bapXcut", view.findViewById<TextView>(R.id.tvOnboardingTitle).text.toString())
    }
}
