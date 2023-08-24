package io.anyline.tiretread.demo.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.anyline.tiretread.demo.AppInfo
import io.anyline.tiretread.demo.HardwareInfo
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.common.StringUtils
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.BuildConfig
import io.anyline.tiretread.sdk.SdkInitializeFailedException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        loadUserSettings(PreferencesUtils.loadDefaultSharedPreferences(this))
        displayHowToGetLicenseInfo()
        displayAppInfo()
    }

    fun onClickedBtnOk(view: View) {
        saveUserSettings()
        finish()
    }

    /**
     * Initializes the SDK and informs the user
     */
    fun onClickedBtnInitializeSDK(view: View) {
        val newLicenseKey = (findViewById<EditText>(R.id.etSettingsLicenseKey)).text.toString()
        try {
            AnylineTireTreadSdk.init(newLicenseKey, this)
            Toast.makeText(this, getString(R.string.txt_setup_correct), Toast.LENGTH_SHORT).show()
        } catch (e: SdkLicenseKeyInvalidException){
            Log.e("SettingsActivity", e.message, e)
            Toast.makeText(this,
                getString(R.string.txt_error_setup_failure_license_key),
                Toast.LENGTH_LONG).show()
        } catch (e: SdkInitializeFailedException){
            Log.e("SettingsActivity", e.message, e)
            Toast.makeText(this,
                getString(R.string.txt_error_setup_failure),
                Toast.LENGTH_LONG).show()
        }
    }

    fun onClickedBtnCancel(view: View) {
        finish()
    }

    private fun loadUserSettings(sharedPref: SharedPreferences) {
        val saveMeasurementData = sharedPref.getBoolean(PreferencesUtils.KEY_MEASUREMENT_DATA, false)
        findViewById<CheckBox>(R.id.cbSettingsSaveMeasurementData).isChecked = saveMeasurementData

        val useImperialSystem = sharedPref.getBoolean(PreferencesUtils.KEY_IMPERIAL_SYSTEM, false)
        findViewById<CheckBox>(R.id.cbSettingsImperialSystem).isChecked = useImperialSystem

        val licenseKey = sharedPref.getString(PreferencesUtils.KEY_LICENSE_KEY, "")
        findViewById<EditText>(R.id.etSettingsLicenseKey).setText(licenseKey)

        val measurementQualityHighSpeed = sharedPref.getBoolean(PreferencesUtils.KEY_MEASUREMENT_QUALITY_HIGH_SPEED, false)
        findViewById<Switch>(R.id.swSettingsMeasurementQuality).isChecked = measurementQualityHighSpeed
    }

    /**
     * Save the user's settings to the SharedPreferences.
     */
    private fun saveUserSettings() {
        val editor = PreferencesUtils.loadDefaultSharedPreferences (this).edit()

        val saveMeasurementData = findViewById<CheckBox>(R.id.cbSettingsSaveMeasurementData).isChecked
        editor.putBoolean(PreferencesUtils.KEY_MEASUREMENT_DATA, saveMeasurementData)

        val useImperialSystem = findViewById<CheckBox>(R.id.cbSettingsImperialSystem).isChecked
        editor.putBoolean(PreferencesUtils.KEY_IMPERIAL_SYSTEM, useImperialSystem)

        val licenseKey = findViewById<EditText>(R.id.etSettingsLicenseKey).text.toString()
        editor.putString(PreferencesUtils.KEY_LICENSE_KEY, licenseKey)

        val measurementQualityHighSpeed = findViewById<Switch>(R.id.swSettingsMeasurementQuality).isChecked
        editor.putBoolean(PreferencesUtils.KEY_MEASUREMENT_QUALITY_HIGH_SPEED, measurementQualityHighSpeed)

        editor.apply()
    }

    /**
     * Show instruction for getting a License Key
     */
    private fun displayHowToGetLicenseInfo() {
        val colorPrimary = ForegroundColorSpan(ContextCompat.getColor(baseContext, R.color.primary))

        val s1 = SpannableString(getString(R.string.txt_settings_howto_license_key_email))
        s1.setSpan(
            URLSpan("mailto:" + getString(R.string.txt_settings_howto_license_key_email)),
            0,
            s1.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        s1.setSpan(colorPrimary, 0, s1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val builder = SpannableStringBuilder()
        builder.append(getString(R.string.txt_settings_howto_license_key) + " ")
        builder.append(s1)
        builder.append(".")

        val tvHowToLicenseKey = findViewById<TextView>(R.id.tvHowToLicenseKey)
        tvHowToLicenseKey.text = builder
        tvHowToLicenseKey.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Show the app's metadata
     */
    private fun displayAppInfo() {
        val appVersion: String = AppInfo.appVersionStr
        val sdkVersion: String = BuildConfig.VERSION_NAME_ANYLINE_TTD_SDK
        val androidApiLevel: Int = HardwareInfo.androidApiLevel

        val versions = String.format(
            "App: %s - SDK: %s - Android API: %s", appVersion, sdkVersion, androidApiLevel
        )
        findViewById<TextView>(R.id.tvSettingsVersion).text = versions

        val deviceName = StringUtils.capitalize(HardwareInfo.getDeviceName())
        findViewById<TextView>(R.id.tvSettingsDeviceName).text = deviceName
    }

}