package io.anyline.tiretread.demo

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import io.anyline.tiretread.demo.common.PreferencesUtils

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @Test
    fun loadSharedPreferences() {
        getInstrumentation().runOnMainSync {
            // Context of the app under test.
            val context = getInstrumentation().targetContext
            val pref: SharedPreferences = PreferencesUtils.loadDefaultSharedPreferences(context)
            assertNotNull(pref)
        }
    }
}