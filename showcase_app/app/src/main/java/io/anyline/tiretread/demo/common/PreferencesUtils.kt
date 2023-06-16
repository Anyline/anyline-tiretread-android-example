package io.anyline.tiretread.demo.common

import android.content.Context
import android.content.SharedPreferences

object PreferencesUtils {

    private const val KEY_SETTINGS_FILE = "io.anyline.tiretread.demo.DEFAULT_FILE_KEY"

    const val KEY_MEASUREMENT_DATA = "measurement_data"
    const val KEY_IMPERIAL_SYSTEM = "imperial_system"
    const val KEY_LICENSE_KEY = "license_key"
    const val KEY_MEASUREMENT_QUALITY_HIGH_SPEED = "measurement_quality_high_speed"

    /**
     * Load the default SharedPreferences.
     *
     * @param context: Context from where the SharedPreferences will be loaded.
     */
    fun loadDefaultSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.packageName.plus(KEY_SETTINGS_FILE), Context.MODE_PRIVATE
        )
    }

    /**
     * Retrieve the License Key defined in the default SharedPreferences.
     * @param context: Context from where the SharedPreferences will be loaded.
     */
    fun getLicenseKey(context: Context): String? {
        return try {
            loadDefaultSharedPreferences(context).getString(KEY_LICENSE_KEY, "")
        } catch (e: java.lang.ClassCastException){
            "";
        }
    }

    /**
     * Retrieve if the Unit System of preference is defined as 'Imperial'.
     * @param context: Context from where the SharedPreferences will be loaded.
     */
    fun shouldUseImperialSystem(context: Context): Boolean {
        return loadDefaultSharedPreferences(context).getBoolean(KEY_IMPERIAL_SYSTEM, false)

    }
}