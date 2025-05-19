package io.anyline.tiretread.devexample.scan_process

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.results.ResultActivity
import io.anyline.tiretread.sdk.scanner.composer.TireTreadScanView

/***
 * Basic example for the use of the TireTreadScanView on a Compose Activity.
 * Docs: https://documentation.anyline.com/tiretreadsdk-component/latest/android/scan-process.html
 * Should be run on a physical device, as Emulators do not support the required capabilities.
 */
class ComposeScanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // This example does not provide any config object to the Scan View.
            // You can check the default values at https://documentation.anyline.com/tiretreadsdk-component/latest/scan-process/overview.html
            TireTreadScanView(
                onScanAborted = ::onScanAborted,
                onScanProcessCompleted = ::openResultScreen,
                callback = null,
                onError = { measurementUUID, exception ->
                    Log.e(getString(R.string.app_name), "Error for $measurementUUID:", exception)
                    Toast.makeText(this, "Failure: ${exception.message}", Toast.LENGTH_LONG).show()
                    finish()
                })
        }
    }

    private fun onScanAborted(measurementUUID: String?) {
        Log.i(
            getString(R.string.app_name),
            "Scan aborted, measurement UUID: ${measurementUUID.toString()}"
        )
        finish()
    }

    private fun openResultScreen(measurementUUID: String) {
        // in this example, we redirect the user to a Result screen, and finish the current Activity
        // obtaining results is a completely isolated process from scanning the tire
        startActivity(Intent(this@ComposeScanActivity, ResultActivity::class.java).apply {
            putExtra("measurementUUID", measurementUUID)
        })
        finish()
    }
}