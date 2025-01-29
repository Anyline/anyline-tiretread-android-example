package io.anyline.tiretread.devexample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import io.anyline.tiretread.sdk.scanner.ScanEvent
import io.anyline.tiretread.sdk.scanner.TireTreadScanViewConfig
import io.anyline.tiretread.sdk.scanner.composer.TireTreadScanView

/***
 * Basic example for the use of the TireTreadScanView on a Compose Activity.
 * Docs: https://documentation.anyline.com/tiretreadsdk-component/latest/android/scan-process.html
 * Should be run on a physical device, as Emulators do not support the required capabilities..
 *
 * The TireTreadScanViewCallback interface is implemented to handle all the callbacks from the
 * TireTreadScanView and the scan process.
 */
class ComposeScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This example provides a default config object to the Scan View.
            // You can check the default values at https://documentation.anyline.com/tiretreadsdk-component/latest/scanconfiguration.html
            val defaultScanViewConfig = TireTreadScanViewConfig()
            ScanViewWithConfigObject(defaultScanViewConfig)
        }
    }

    /**
     * Initializes a new TireTreadScanView with a TireTreadScanViewConfig object
     * @param scanViewConfig The configuration object for the TireTreadScanView.
     *
     * @see "https://documentation.anyline.com/tiretreadsdk-component/latest/android/scan-process.html#setup-with-a-config-object"
     */
    @Composable
    private fun ScanViewWithConfigObject(scanViewConfig: TireTreadScanViewConfig) {
        TireTreadScanView(config = scanViewConfig,
            tireWidth = null, // if tireWidth is not provided, the Tire Width Selection screen will be displayed automatically
            onScanAborted = ::onScanAborted,
            /*
             * Invoked once all the frames were successfully uploaded for processing.
             * Once this callback is invoked, your application should take back the control of the workflow.
             * Use this to, e.g. finish this activity, redirect the user to a result screen, start fetching results in the background, etc.
             */
            onScanProcessCompleted = ::openResultScreen,
            callback = ::handleScanEvent, // sets this function as the scan event listener
            // This is invoked whenever anything fails during the initialization of the Scan View.
            // If something fails, your application should redirect the user flow to avoid the scanner.
            onError = { measurementUUID, exception ->
                Log.e(getString(R.string.app_name), "Error for $measurementUUID:", exception)
                Toast.makeText(this, "Failure: ${exception.message}", Toast.LENGTH_LONG).show()
                finish()
            })
    }

    private fun onScanAborted(measurementUUID: String?) {
        Log.i(
            getString(R.string.app_name),
            "Scan aborted, measurement UUID: ${measurementUUID.toString()}"
        )
        finish()
    }

    /**
     * docs: https://documentation.anyline.com/tiretreadsdk-component/latest/android/scan-process.html#android_sdk_callbacks
     */
    private fun handleScanEvent(event: ScanEvent) {
        when (event) {

            // When using the Default UI, other callbacks can safely be ignored.
            else -> {
                Log.i(getString(R.string.app_name), event.toString())
            }
        }
    }

    private fun openResultScreen(uuid: String) {
        // in this example, we redirect the user to a Result screen, and finish the current Activity.
        startActivity(Intent(this@ComposeScanActivity, ResultActivity::class.java).apply {
            putExtra("uuid", uuid)
        })
        finish()
    }
}