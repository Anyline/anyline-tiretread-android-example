package io.anyline.tiretread.demo.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.demo.AppInfo
import io.anyline.tiretread.demo.CustomBottomSheet
import io.anyline.tiretread.demo.HardwareInfo
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.common.StringUtils
import io.anyline.tiretread.demo.common.makeLinks
import io.anyline.tiretread.demo.databinding.ActivitySettingsBinding
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.BuildConfig
import io.anyline.tiretread.sdk.SdkInitializeFailedException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpUi()
    }

    private fun setUpUi() {
        val sharedPref = PreferencesUtils.loadDefaultSharedPreferences(this)

        binding.cbSettingsImperialSystem.isChecked =
            sharedPref.getBoolean(PreferencesUtils.KEY_IMPERIAL_SYSTEM, false)

        binding.etSettingsLicenseKey.setText(
            sharedPref.getString(
                PreferencesUtils.KEY_LICENSE_KEY, ""
            )
        )

        binding.tvSettingsDeviceName.text = StringUtils.capitalize(HardwareInfo.getDeviceName())

        binding.tvSettingsVersion.text = getString(
            R.string.settings_screen_versions,
            AppInfo.appVersionStr,
            BuildConfig.VERSION_NAME_ANYLINE_TTD_SDK,
            HardwareInfo.androidApiLevel.toString()
        )

        binding.tvHowToLicenseKey.makeLinks("presales@anyline.com")

        setUpScanSpeedTextView()
        setUpButtons()
    }

    private fun setUpScanSpeedTextView() {
        val isFastSpeedEnabled = PreferencesUtils.isFastScanSpeedSet(this)
        updateScanSpeedTextViewText(isFastSpeedEnabled)
    }

    private fun openBottomSheet(isFastScanModeEnabled: Boolean) {
        CustomBottomSheet(
            "Select scan speed",
            listOf(
                CustomBottomSheet.BottomSheetOption(0, "Fast"),
                CustomBottomSheet.BottomSheetOption(1, "Slow")
            ),
            object : CustomBottomSheet.CustomBottomSheetListener {

                override fun getCurrentSelectedOption(): Int = when (isFastScanModeEnabled) {
                    true -> 0
                    false -> 1
                }

                override fun onCustomBottomOptionSelected(selectedOption: CustomBottomSheet.BottomSheetOption) {
                    val isFastScanModeEnabled = selectedOption.value == 0
                    PreferencesUtils.setScanSpeed(this@SettingsActivity, isFastScanModeEnabled)
                    updateScanSpeedTextViewText(isFastScanModeEnabled)
                }
            })
            .show(supportFragmentManager, "bottom_sheet")
    }

    private fun updateScanSpeedTextViewText(isFastScanModeEnabled: Boolean) {
        binding.scanSpeedTextView.apply {
            val text = if (isFastScanModeEnabled) {
                getString(R.string.settings_screen_fast)
            } else {
                getString(R.string.settings_screen_slow)
            }
            this.text = text

            makeLinks(
                text to View.OnClickListener {
                    openBottomSheet(isFastScanModeEnabled)
                }
            )
        }
    }

    private fun setUpButtons() {
        binding.btnSettingsOk.setOnClickListener {
            saveUserSettings()
            finish()
        }

        binding.btnInitializeSDK.setOnClickListener {
            initialiseSdk()
        }

        binding.btnSettingsCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveUserSettings() {
        val editor = PreferencesUtils.loadDefaultSharedPreferences(this).edit()

        val useImperialSystem = findViewById<CheckBox>(R.id.cbSettingsImperialSystem).isChecked
        editor.putBoolean(PreferencesUtils.KEY_IMPERIAL_SYSTEM, useImperialSystem)

        val licenseKey = findViewById<EditText>(R.id.etSettingsLicenseKey).text.toString()
        editor.putString(PreferencesUtils.KEY_LICENSE_KEY, licenseKey)

        editor.apply()
    }

    private fun initialiseSdk() {
        val newLicenseKey = (findViewById<EditText>(R.id.etSettingsLicenseKey)).text.toString()
        try {
            AnylineTireTreadSdk.init(newLicenseKey, this)
            Toast.makeText(this, getString(R.string.txt_setup_correct), Toast.LENGTH_SHORT).show()
        } catch (e: SdkLicenseKeyInvalidException) {
            Log.e("SettingsActivity", e.message, e)
            Toast.makeText(
                this, getString(R.string.txt_error_setup_failure_license_key), Toast.LENGTH_LONG
            ).show()
        } catch (e: SdkInitializeFailedException) {
            Log.e("SettingsActivity", e.message, e)
            Toast.makeText(
                this, getString(R.string.txt_error_setup_failure), Toast.LENGTH_LONG
            ).show()
        }
    }
}