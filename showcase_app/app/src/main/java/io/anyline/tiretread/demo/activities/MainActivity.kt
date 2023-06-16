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
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.CustomTypefaceSpan
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.SdkInitializeFailedException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException


private const val CAMERA_REQUEST_CODE = 200

class MainActivity : AppCompatActivity() {
    private var shouldOpenScanActivity : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!isCameraPermissionGranted()){
            requestCameraPermission(false)
        }

        displayWelcomeMessages()
    }

    /**
     * Open Scan Activity
     */
    fun onClickedBtnStart(view: View) {
        if(!isCameraPermissionGranted()){
            requestCameraPermission(true)
            return
        }
        if(initializeAnylineTiretreadSdk()){
            openScanActivity()
        }
    }

    private fun initializeAnylineTiretreadSdk() : Boolean {

        val licenseKey : String? = PreferencesUtils.getLicenseKey(this)
        var errorMessage = "";
        if(licenseKey != null) {
            // Initialize the SDK
            errorMessage = try {
                AnylineTireTreadSdk.init(licenseKey, this)
                Log.i("Init SDK", "Success")
                return true
            } catch (e: SdkLicenseKeyInvalidException) {
                getString(R.string.txt_error_invalid_license_key)
            } catch (e: SdkInitializeFailedException){
                getString(R.string.txt_error_setup_failure)
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", errorMessage);
        return false
    }

    private fun requestCameraPermission(shouldOpenScanActivity: Boolean) {
        this.shouldOpenScanActivity = shouldOpenScanActivity
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    /**
     * Open the Scan Activity
     */
    private fun openScanActivity(){
        val intent = Intent(this, ScanActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    /**
     * Open the Settings Activity
     */
    fun onClickedBtnSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun displayWelcomeMessages() {
        val fontLight = ResourcesCompat.getFont(baseContext, R.font.proxima_nova_light)
        val fontBold = ResourcesCompat.getFont(baseContext, R.font.proxima_nova_bold)
        val colorPrimary = ForegroundColorSpan(ContextCompat.getColor(baseContext, R.color.primary))

        val s1 = SpannableString(getString(R.string.txt_main_to_start_scanning_press_the) + " ")
        val s2 = SpannableString(getString(R.string.btn_start_uc) + " ")
        val s3 = SpannableString(getString(R.string.txt_main_button_dot))

        s1.setSpan(CustomTypefaceSpan(fontLight), 0, s1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s2.setSpan(CustomTypefaceSpan(fontBold), 0, s2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s2.setSpan(colorPrimary,0, s2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s3.setSpan(CustomTypefaceSpan(fontLight), 0, s3.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val builderMessage = SpannableStringBuilder()
        builderMessage.append(s1)
        builderMessage.append(s2)
        builderMessage.append(s3)

        findViewById<TextView>(R.id.tvWelcomeMessage).text = builderMessage

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

        val welcomeDocumentation = findViewById<TextView>(R.id.tvWelcomeDocumentation)
        welcomeDocumentation.text = builderDoc
        welcomeDocumentation.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Check if the Camera Permission is already granted.
     */
    private fun isCameraPermissionGranted() : Boolean {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request the camera permission.
     * Calls openScanActivity() if 'shouldOpenScanActivity' is 'true'.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if(requestCode != CAMERA_REQUEST_CODE) { return }
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (this.shouldOpenScanActivity){
                    openScanActivity()
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED) {
                    showDialogOKCancel("To be able to scan, provide the camera permission.") {
                        _, which ->
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            intent.data = Uri.fromParts("package", packageName, null)
                            startActivity(intent)
                        }
                        else {
                            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun showDialogOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}