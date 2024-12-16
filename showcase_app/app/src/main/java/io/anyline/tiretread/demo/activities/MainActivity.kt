package io.anyline.tiretread.demo.activities

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.CustomTypefaceSpan
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.common.makeLinks
import io.anyline.tiretread.demo.databinding.ActivityMainBinding
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CAMERA_REQUEST_CODE = 200

class MainActivity : AppCompatActivity() {
    private var shouldOpenScanActivity: Boolean = false

    private lateinit var binding: ActivityMainBinding
    private var shouldShowTutorial = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isCameraPermissionGranted()) {
            requestCameraPermission(false)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            shouldShowTutorial = PreferencesUtils.shouldShowTutorial(this@MainActivity)
        }

        binding.btnMainStart.setOnClickListener {
            startScanning()
        }

        binding.btnMainTutorial.setOnClickListener {
            goToTutorialScreen()
        }

        binding.btnMainSettings.setOnClickListener {
            goToSettingsScreen()
        }

        binding.tutorialInfoTextView.makeLinks(Pair("TUTORIAL", object : View.OnClickListener {
            override fun onClick(v: View?) {
                goToTutorialScreen()
            }
        }))

        binding.tvWelcomeMessage.makeLinks(Pair("OPEN SCANNER", object : View.OnClickListener {
            override fun onClick(v: View?) {
                startScanning()
            }
        }))

        displayWelcomeMessages()
    }

    override fun onPostResume() {
        super.onPostResume()
        binding.btnMainStart.isEnabled = true
    }

    private fun startScanning() {
        binding.btnMainStart.setText(R.string.main_activity_opening)
        binding.btnMainStart.isEnabled = false

        if (!isCameraPermissionGranted()) {
            requestCameraPermission(shouldOpenScanActivity = AnylineTireTreadSdk.isInitialized)
            binding.btnMainStart.setText(R.string.btn_start)
            return
        }

        if (!shouldShowTutorial) {
            goToTutorialScreen()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (initializeAnylineTireTreadSdk()) {
                openScanActivity()
            } else {
                withContext(Dispatchers.Main) {
                    binding.btnMainStart.setText(R.string.btn_start)
                    binding.btnMainStart.isEnabled = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnMainStart.setText(R.string.btn_start)
    }

    private fun goToTutorialScreen() {
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }

    private fun initializeAnylineTireTreadSdk(): Boolean {
        return TireTreadSdkInitializer.initSdk(this, PreferencesUtils.getLicenseKey(this) ?: "")
    }

    private fun requestCameraPermission(shouldOpenScanActivity: Boolean) {
        this.shouldOpenScanActivity = shouldOpenScanActivity
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE
        )
    }

    /**
     * Open the Scan Activity
     */
    private fun openScanActivity() {
        val intent = Intent(this, ScanActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    /**
     * Open the Settings Activity
     */
    fun goToSettingsScreen() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun displayWelcomeMessages() {
        val fontLight = ResourcesCompat.getFont(baseContext, R.font.proxima_nova_light)

        val s4 =
            SpannableString(getString(R.string.txt_main_for_best_scanning_practices_please_go_to) + " ")
        val s5 = SpannableString(getString(R.string.txt_main_tire_tread_docu_anyline_com))

        s4.setSpan(CustomTypefaceSpan(fontLight), 0, s4.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s5.setSpan(
            URLSpan("https://tiretreaddocu.anyline.com/"),
            0,
            s5.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        s5.setSpan(ForegroundColorSpan(Color.BLACK), 0, s5.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val builderDoc = SpannableStringBuilder()
        builderDoc.append(s4)
        builderDoc.append(s5)

        binding.tvWelcomeDocumentation.apply {
            text = builderDoc
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    /**
     * Check if the Camera Permission is already granted.
     */
    private fun isCameraPermissionGranted(): Boolean {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request the camera permission.
     * Calls openScanActivity() if 'shouldOpenScanActivity' is 'true'.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != CAMERA_REQUEST_CODE) {
            return
        }
        if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            if (this.shouldOpenScanActivity) {
                openScanActivity()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_DENIED
            ) {
                showDialogOKCancel("To be able to scan, provide the camera permission.") { _, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        intent.data = Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showDialogOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null).create().show()
    }
}