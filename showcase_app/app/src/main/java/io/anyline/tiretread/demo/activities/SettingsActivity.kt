package io.anyline.tiretread.demo.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.demo.AppInfo
import io.anyline.tiretread.demo.CustomBottomSheet
import io.anyline.tiretread.demo.HardwareInfo
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.common.StringUtils
import io.anyline.tiretread.demo.common.makeLinks
import io.anyline.tiretread.demo.databinding.ActivitySettingsBinding
import io.anyline.tiretread.sdk.BuildConfig


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpUi()

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE_RESULT)?.let {
                    binding.etSettingsLicenseKey.setText(it)
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(
                    this, "Barcode scanning was cancelled. No license key found!", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setUpUi() {
        val sharedPref = PreferencesUtils.loadDefaultSharedPreferences(this)

        binding.cbSettingsImperialSystem.isChecked =
            sharedPref.getBoolean(PreferencesUtils.KEY_IMPERIAL_SYSTEM, false)

        binding.deviceCompatibilityModeCheckBox.isChecked =
            PreferencesUtils.shouldUseDeviceCompatibilityMode(this)

        binding.showGuidanceCheckBox.isChecked = PreferencesUtils.shouldShowOverlay(this)

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

        binding.scanBarcodeButton.setOnClickListener {
            activityResultLauncher.launch(ScanBarcodeActivity.newIntent(this))
        }

        binding.customTagEditText.setText(PreferencesUtils.getCustomTag(this))

        setUpScanSpeedTextView()
        setUpButtons()
    }

    private fun setUpScanSpeedTextView() {
        val isFastSpeedEnabled = PreferencesUtils.isFastScanSpeedSet(this)
        updateScanSpeedTextViewText(isFastSpeedEnabled)
    }

    private fun openBottomSheet(isFastScanModeEnabled: Boolean) {
        CustomBottomSheet("Select scan speed", listOf(
            CustomBottomSheet.BottomSheetOption(0, "Fast"),
            CustomBottomSheet.BottomSheetOption(1, "Slow")
        ), object : CustomBottomSheet.CustomBottomSheetListener {

            override fun getCurrentSelectedOption(): Int = when (isFastScanModeEnabled) {
                true -> 0
                false -> 1
            }

            override fun onCustomBottomOptionSelected(selectedOption: CustomBottomSheet.BottomSheetOption) {
                val isFastScanModeEnabled = selectedOption.value == 0
                PreferencesUtils.setScanSpeed(this@SettingsActivity, isFastScanModeEnabled)
                updateScanSpeedTextViewText(isFastScanModeEnabled)
            }
        }).show(supportFragmentManager, "bottom_sheet")
    }

    private fun updateScanSpeedTextViewText(isFastScanModeEnabled: Boolean) {
        binding.scanSpeedTextView.apply {
            val text = if (isFastScanModeEnabled) {
                getString(R.string.settings_screen_fast)
            } else {
                getString(R.string.settings_screen_slow)
            }
            this.text = text

            makeLinks(text to View.OnClickListener {
                openBottomSheet(isFastScanModeEnabled)
            })
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

        val showOverlay = binding.showGuidanceCheckBox.isChecked
        PreferencesUtils.showOverlay(this, showOverlay)

        PreferencesUtils.setShouldUseDeviceCompatibilityMode(
            this, binding.deviceCompatibilityModeCheckBox.isChecked
        )

        binding.customTagEditText.text.toString().let {
            if (it.isNotEmpty()) {
                PreferencesUtils.setCustomTag(this, it)
            }
        }
    }

    private fun initialiseSdk() {
        val newLicenseKey = (findViewById<EditText>(R.id.etSettingsLicenseKey)).text.toString()
        TireTreadSdkInitializer.initSdk(this, newLicenseKey) {
            Toast.makeText(
                this, getString(R.string.txt_setup_correct), Toast.LENGTH_SHORT
            ).show()
        }
    }
}