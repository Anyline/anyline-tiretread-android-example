package io.anyline.tiretread.demo.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_UP
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.databinding.ActivityScanBinding
import io.anyline.tiretread.sdk.scanner.AnylineInternalFeature
import io.anyline.tiretread.sdk.scanner.DistanceStatus
import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.scanner.ScanSpeed
import io.anyline.tiretread.sdk.scanner.TireTreadScanViewCallback
import io.anyline.tiretread.sdk.scanner.TireTreadScanner

class ScanActivity : AppCompatActivity(), TireTreadScanViewCallback {

    private lateinit var mainHandler: Handler
    private lateinit var binding: ActivityScanBinding

    private var aborted = false

    @OptIn(AnylineInternalFeature::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start UI update loop
        Handler(this.mainLooper).removeCallbacksAndMessages(null)
        mainHandler = Handler(this.mainLooper)

        // Configure the TireTreadScanView
        binding.tireTreadScanView.apply {
            scanViewCallback = this@ScanActivity
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(
                    this@ScanActivity
                )
            )
            measurementSystem = if (PreferencesUtils.shouldUseImperialSystem(this@ScanActivity)) {
                MeasurementSystem.Imperial
            } else {
                MeasurementSystem.Metric
            }

            // This API (scanSpeed) is experimental, may impact scan performance and be removed with any major SDK release.
            // You are advised to ignore this configuration on your implementation.
            if (PreferencesUtils.isFastScanSpeedSet(this@ScanActivity)) {
                setScanSpeed(ScanSpeed.Fast)
            } else {
                setScanSpeed(ScanSpeed.Slow)
            }
        }
    }

    override fun onScanAbort(uuid: String?) {
        super.onScanAbort(uuid)
        aborted = true
        finish()
    }

    override fun onUploadCompleted(uuid: String?) {
        super.onUploadCompleted(uuid)

        if (!aborted && !uuid.isNullOrEmpty()) {
            if (PreferencesUtils.shouldRequestTireId(this)) {
                // This function is only intended for feedback and does not need to be implemented.
                openBarcodeScanner(uuid)
            } else {
                openLoadAndResultScreen(uuid)
            }
        }
    }

    override fun onUploadAborted(uuid: String?) {
        super.onUploadAborted(uuid)
        Log.i("Showcase", "Upload Aborted")
        finish()
    }

    override fun onUploadFailed(uuid: String?, exception: Exception) {
        super.onUploadFailed(uuid, exception)
        mainHandler.post {
            Toast.makeText(applicationContext, "Upload Failed", Toast.LENGTH_SHORT).show()
        }
        finish()
    }


    private fun openLoadAndResultScreen(uuid: String) {
        val intent = Intent(this, MeasurementResultActivity::class.java).apply {
            putExtra("measurement_uuid", uuid)
        }
        startActivity(intent)
        finish()
    }


    // This function is only intended for feedback and does not need to be implemented.
    private fun openBarcodeScanner(uuid: String?) {
        startActivity(ScanBarcodeActivity.newIntentForRecorder(this, uuid ?: ""))
        finish()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == ACTION_UP) return true

                if (TireTreadScanner.instance.isScanning) {
                    TireTreadScanner.instance.stopScanning()
                } else {
                    if (TireTreadScanner.instance.captureDistanceStatus == DistanceStatus.OK)
                        TireTreadScanner.instance.startScanning()
                    else {
                        Toast.makeText(
                            this,
                            "Move the phone to the correct position before starting.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                true
            }

            else -> super.dispatchKeyEvent(event)
        }
    }
}