package io.anyline.tiretread.devexample.scan_process

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.databinding.ActivityXmlScanBinding
import io.anyline.tiretread.devexample.results.ResultActivity

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
        binding = ActivityXmlScanBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        // This example does not provide any config object to the Scan View.
        // You can check the default values at https://documentation.anyline.com/tiretreadsdk-component/latest/scan-process/overview.html
        binding.scanView.init(
            onScanAborted = ::onScanAborted,
            onScanProcessCompleted = ::openResultScreen,
            tireTreadScanViewCallback = null,
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

    private fun openResultScreen(uuid: String) {
        // in this example, we redirect the user to a Result screen, and finish the current Activity
        // obtaining results is a completely isolated process from scanning the tire
        startActivity(Intent(this@XmlScanActivity, ResultActivity::class.java).apply {
            putExtra("measurementUUID", uuid)
        })
        finish()
    }
}