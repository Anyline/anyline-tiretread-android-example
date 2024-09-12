package io.anyline.tiretread.demo.common

import android.content.Context
import android.content.SharedPreferences

object PreferencesUtils {

    private const val KEY_SETTINGS_FILE = "io.anyline.tiretread.demo.DEFAULT_FILE_KEY"

    const val KEY_MEASUREMENT_DATA = "measurement_data"
    const val KEY_IMPERIAL_SYSTEM = "imperial_system"
    const val KEY_LICENSE_KEY = "license_key"
    private const val KEY_TUTORIAL_SHOWN = "tutorial_shown"
    const val KEY_MEASUREMENT_QUALITY_HIGH_SPEED = "measurement_quality_high_speed"
    private const val KEY_SCAN_SPEED = "KEY_SCAN_SPEED"
    private const val KEY_IS_RECORDER = "KEY_IS_RECORDER"
    private const val KEY_SHOW_OVERLAY = "KEY_SHOW_OVERLAY"
    private const val KEY_SHOW_TIRE_WIDTH = "KEY_SHOW_TIRE_WIDTH"

    /**
     * Load the default SharedPreferences.
     *
     * @param context: Context where the SharedPreferences will be loaded from.
     */
    fun loadDefaultSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.packageName.plus(KEY_SETTINGS_FILE), Context.MODE_PRIVATE
        )
    }

    /**
     * Load the a SharedPreferences according to the fileKey provided.
     *
     * @param context: Context from where the SharedPreferences will be loaded.
     * @param fileKey: Key for the specific shared preference
     */
    fun loadSharedPreferences(context: Context, fileKey: String): SharedPreferences {
        return context.getSharedPreferences(
            context.packageName.plus(fileKey), Context.MODE_PRIVATE
        )
    }

    /**
     * Retrieve the License Key defined in the default SharedPreferences.
     * @param context: Context from where the SharedPreferences will be loaded.
     */
    fun getLicenseKey(context: Context): String? {
        return try {
            loadDefaultSharedPreferences(context).getString(KEY_LICENSE_KEY, "")
        } catch (e: java.lang.ClassCastException) {
            ""
        }
    }

    /**
     * Retrieve if the Unit System of preference is defined as 'Imperial'.
     * @param context: Context from where the SharedPreferences will be loaded.
     */
    fun shouldUseImperialSystem(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_IMPERIAL_SYSTEM, false)
    }

    fun shouldShowTutorial(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_TUTORIAL_SHOWN, false)
    }

    fun onTutorialShown(context: Context) {
        loadDefaultSharedPreferences(context).edit().putBoolean(KEY_TUTORIAL_SHOWN, true).apply()
    }

    fun setScanSpeed(context: Context, isFast: Boolean) {
        loadDefaultSharedPreferences(context).edit().putBoolean(KEY_SCAN_SPEED, isFast).apply()
    }

    fun isFastScanSpeedSet(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_SCAN_SPEED, true)
    }

    // This function is only intended for feedback and does not need to be implemented.
    fun shouldRequestTireId(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_IS_RECORDER, false)
    }

    // This function is only intended for feedback and does not need to be implemented.
    fun setShouldRequestTireId(context: Context, isRecorder: Boolean) {
        loadDefaultSharedPreferences(context).edit().putBoolean(KEY_IS_RECORDER, isRecorder).apply()
    }

    fun showOverlay(context: Context, showOverlay: Boolean) {
        loadDefaultSharedPreferences(context).edit().putBoolean(KEY_SHOW_OVERLAY, showOverlay)
            .apply()
    }

    fun shouldShowOverlay(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_SHOW_OVERLAY, true)
    }

    /**
     * Adds a new Tire Registration to the SharedPreferences.
     *
     * @param context Context where the SharedPreferences will be loaded from.
     * @param licenseKey The LicenseKey for which the Tire Registration will be added
     * @param tireId The TireId being registered
     */
    fun addNewTireRegistration(context: Context, licenseKey: String, tireId: String) {
        val licensesSharedPreference = loadSharedPreferences(context, licenseKey)
        var tireRegistrations = licensesSharedPreference.getInt(tireId, 0)
        tireRegistrations++
        licensesSharedPreference.edit().putInt(tireId, tireRegistrations).apply()
    }

    /**
     * Adds a new Tire Registration to the SharedPreferences.
     *
     * @param context Context where the SharedPreferences will be loaded from.
     * @param tireId The TireId being registered
     */
    fun addNewTireRegistrationToCurrentLicenseKey(context: Context, tireId: String) {
        val licenseKey = getLicenseKey(context) ?: ""
        addNewTireRegistration(context, licenseKey, tireId)
    }

    /**
     * Loads the Tire Registration Count from the SharedPreferences.
     *
     * @param context Context where the SharedPreferences will be loaded from.
     * @param licenseKey The LicenseKey for which the Tire Registration will loaded
     * @param tireId The TireId being checked
     */
    fun loadTireRegistrationCount(context: Context, licenseKey: String, tireId: String): Int {
        return loadSharedPreferences(context, licenseKey).getInt(tireId, 0)
    }

    /**
     * Loads the Tire Registration Count from the SharedPreferences for the current License Key.
     *
     * @param context Context where the SharedPreferences will be loaded from.
     * @param tireId The TireId being checked
     */
    fun loadTireRegistrationCountForCurrentLicenseKey(context: Context, tireId: String): Int {
        val licenseKey = getLicenseKey(context) ?: ""
        return loadTireRegistrationCount(context, licenseKey, tireId)
    }

    fun shouldShowTireWidthDialog(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_SHOW_TIRE_WIDTH, true)
    }

    fun setShouldShowTireWidthDialog(context: Context, value: Boolean) {
        loadDefaultSharedPreferences(context).edit().putBoolean(KEY_SHOW_TIRE_WIDTH, value).apply()
    }
}