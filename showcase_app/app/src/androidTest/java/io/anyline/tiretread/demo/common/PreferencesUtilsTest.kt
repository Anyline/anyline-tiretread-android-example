package io.anyline.tiretread.demo.common

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesUtilsTest {

    companion object {
        private val context = getInstrumentation().targetContext
        private const val LICENSE_KEY_1 = "1234567890"
        private const val TIRE_ID_1 = "AB-123"
        private const val LICENSE_KEY_2 = "0987654321"
        private const val TIRE_ID_2 = "ZY-321"
    }

    @Before
    fun setup() {
        // Clear SharedPreferences
        PreferencesUtils.loadSharedPreferences(context, LICENSE_KEY_1).edit().clear().apply()
        PreferencesUtils.loadSharedPreferences(context, LICENSE_KEY_2).edit().clear().apply()
    }

    @Test
    fun loadSharedDefaultPreferences() {
        getInstrumentation().runOnMainSync {
            val pref: SharedPreferences = PreferencesUtils.loadDefaultSharedPreferences(context)
            assertNotNull(pref)
        }
    }

    @Test
    fun when_addingNewTireRegistration_shouldIncrementTireRegistrationCountByOne() {
        // Add new registration
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_1, TIRE_ID_1)
        val count = PreferencesUtils.loadTireRegistrationCount(context, LICENSE_KEY_1, TIRE_ID_1)

        // Assert
        assertEquals(1, count)
    }

    @Test
    fun when_addingNewTireRegistration_twice_shouldIncrementTireRegistrationCountByTwo() {
        // Add new registrations
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_1, TIRE_ID_1)
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_1, TIRE_ID_1)
        val count = PreferencesUtils.loadTireRegistrationCount(context, LICENSE_KEY_1, TIRE_ID_1)

        // Assert
        assertEquals(2, count)
    }

    @Test
    fun when_addingDifferentTireIdRegistration_shouldIncrementTireRegistrationCountByOne() {
        // Add new registrations
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_1, TIRE_ID_1)
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_1, TIRE_ID_2)
        val count = PreferencesUtils.loadTireRegistrationCount(context, LICENSE_KEY_1, TIRE_ID_2)

        // Assert
        assertEquals(1, count)
    }

    @Test
    fun when_addingNewTireRegistrationToNewLicenseKey_shouldIncrementTireRegistrationCountByOne() {
        // Add new registrations
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_1, TIRE_ID_1)
        PreferencesUtils.addNewTireRegistration(context, LICENSE_KEY_2, TIRE_ID_1)
        val countLicense2 =
            PreferencesUtils.loadTireRegistrationCount(context, LICENSE_KEY_2, TIRE_ID_1)

        // Assert
        assertEquals(1, countLicense2)
    }

    @Test
    fun when_loadingTireRegistrationCount_withoutPreviousRegistries_shouldReturnZero() {
        PreferencesUtils.loadSharedPreferences(context, LICENSE_KEY_1).edit().clear().apply()

        // Load registries count
        val count = PreferencesUtils.loadTireRegistrationCount(context, LICENSE_KEY_1, TIRE_ID_1)

        // Assert
        assertEquals(0, count)
    }
}