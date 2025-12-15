package io.anyline.tiretread.devexample.scan_process

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.databinding.ActivityXmlScanBinding
import io.anyline.tiretread.devexample.results.ResultActivity
import io.anyline.tiretread.sdk.scanner.ScanEvent
import io.anyline.tiretread.sdk.scanner.TireTreadScanner

/***
 * Basic example for the use of the TireTreadScanView with XML Layouts.
 * Docs: https://documentation.anyline.com/tiretreadsdk-component/latest/android/scan-process.html
 * Should be run on a physical device, as Emulators do not support the required capabilities.
 */
class XmlScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityXmlScanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        binding = ActivityXmlScanBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        // Check if JSON config path was provided
        val jsonConfigPath = intent.getStringExtra("jsonConfigPath")

        if (jsonConfigPath != null) {
            // This example uses a JSON configuration file for the Scan View.
            // The JSON config path is passed from the JsonConfigSelectionActivity.
            binding.scanView.init(
                tireTreadConfig = jsonConfigPath,
                onScanAborted = ::onScanAborted,
                onScanProcessCompleted = ::openResultScreen,
                tireTreadScanViewCallback = ::handleScanEvent,
                onError = { measurementUUID, exception ->
                    Log.e(getString(R.string.app_name), "Error for $measurementUUID:", exception)
                    Toast.makeText(this, "Failure: ${exception.message}", Toast.LENGTH_LONG).show()
                    finish()
                })
        } else {
            // This example does not provide any config object to the Scan View,
            //      which means that a default configuration will be used.
            // You can check the default values at https://documentation.anyline.com/tiretreadsdk-component/latest/scan-process/overview.html
            binding.scanView.init(
                onScanAborted = ::onScanAborted,
                onScanProcessCompleted = ::openResultScreen,
                tireTreadScanViewCallback = ::handleScanEvent,
                onError = { measurementUUID, exception ->
                    Log.e(getString(R.string.app_name), "Error for $measurementUUID:", exception)
                    Toast.makeText(this, "Failure: ${exception.message}", Toast.LENGTH_LONG).show()
                    finish()
                })
        }

        // Log the current configuration as JSON (for debug purposes only)
        try {
            val configJson = TireTreadScanner.getTireTreadConfigAsJson()
            Log.i(getString(R.string.app_name), "TireTreadConfig in use: $configJson")
        } catch (e: Exception) {
            Log.w(getString(R.string.app_name), "Could not get config JSON: ${e.message}")
        }
    }

    private fun handleScanEvent(event: ScanEvent) {
        Log.i(getString(R.string.app_name), event.toString())
    }

    private fun onScanAborted(measurementUUID: String?) {
        Log.i(
            getString(R.string.app_name),
            "Scan aborted, measurement UUID: ${measurementUUID.toString()}"
        )
        finish()
    }

    private fun openResultScreen(uuid: String) {
        // in this example, we redirect the user to a Result screen, and finish the current Activity
        // obtaining results is a completely isolated process from scanning the tire
        startActivity(Intent(this@XmlScanActivity, ResultActivity::class.java).apply {
            putExtra("measurementUUID", uuid)
        })
        finish()
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Hide both navigation bar and status bar
            hide(WindowInsetsCompat.Type.systemBars())
            // Make bars appear temporarily on swipe, then auto-hide
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}