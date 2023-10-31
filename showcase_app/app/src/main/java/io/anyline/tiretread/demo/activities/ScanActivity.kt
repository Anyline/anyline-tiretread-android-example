package io.anyline.tiretread.demo.activities

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.databinding.ActivityScanBinding
import io.anyline.tiretread.sdk.scanner.DistanceStatus
import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.scanner.TireTreadScanViewCallback
import io.anyline.tiretread.sdk.scanner.TireTreadScanner
import io.anyline.tiretread.sdk.utils.inchStringToTriple
import io.anyline.tiretread.sdk.utils.inchToFractionString

class ScanActivity : AppCompatActivity(), TireTreadScanViewCallback {

    private lateinit var mainHandler: Handler
    private lateinit var binding: ActivityScanBinding
    private lateinit var countDownTimer: CountDownTimer

    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var aborted = false
    private var maxScanDuration: Long = 0

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
        }
        binding.tmpOverlay.visibility = View.GONE

        binding.btnAbort.setOnClickListener {
            abortScan()
        }

        maxScanDuration = if (PreferencesUtils.isFastScanSpeedSet(this)) {
            MAX_DURATION_SCAN_SPEED_FAST_MILLIS
        } else {
            MAX_DURATION_SCAN_SPEED_SLOW_MILLIS
        }

        binding.pbProgress.max = maxScanDuration.toInt()

        countDownTimer = object : CountDownTimer(maxScanDuration, 500L) {

            override fun onTick(millisUntilFinished: Long) {
                binding.pbProgress.progress = millisUntilFinished.toInt()
            }

            override fun onFinish() {
                if (maxScanDuration == MAX_DURATION_SCAN_SPEED_FAST_MILLIS) {
                    stopScanning()
                }
            }
        }
    }

    fun onClickedBtnStartScan(view: View) {
        val scannerInstance = TireTreadScanner.instance

        val btnStartScan = (view as Button)
        if (!scannerInstance.isScanning) {
            scannerInstance.startScanning()
            btnStartScan.text = "Stop"
        } else {
            stopScanning()
        }
    }

    private fun stopScanning() {
        with(binding.btnStartScan) {
            visibility = View.GONE
            isEnabled = false
            isClickable = false
        }
        TireTreadScanner.instance.stopScanning()
    }

    private fun abortScan() {
        TireTreadScanner.instance.abortScanning()
        aborted = true
        countDownTimer.cancel()
        mediaPlayer = MediaPlayer.create(baseContext, R.raw.sound_stop)
        mediaPlayer.start()
        finish()
    }

    override fun onScanStart(uuid: String?) {
        super.onScanStart(uuid)

        mediaPlayer = MediaPlayer.create(baseContext, R.raw.sound_start)
        mediaPlayer.start()

        binding.pbProgress.visibility = View.VISIBLE
        binding.pbProgress.progress = maxScanDuration.toInt()
        countDownTimer.start()
    }

    override fun onScanStop(uuid: String?) {
        super.onScanStop(uuid)
        mediaPlayer = MediaPlayer.create(baseContext, R.raw.sound_stop)
        mediaPlayer.start()

        binding.pbProgress.progress = 0
        countDownTimer.cancel()

        mainHandler.post {
            hideStartButton()
            binding.tmpOverlay.visibility = View.VISIBLE
        }
    }

    private fun hideStartButton() {
        binding.btnStartScan.apply {
            visibility = View.GONE
            isEnabled = false
            isClickable = false
        }
    }

    override fun onUploadCompleted(uuid: String?) {
        super.onUploadCompleted(uuid)

        if (!aborted && !uuid.isNullOrEmpty()) {
            openLoadAndResultScreen(uuid)
        }
    }

    private fun openLoadAndResultScreen(uuid: String) {
        val bundle = Bundle()
        bundle.putString("measurement_uuid", uuid)

        val intent = Intent(this, MeasurementResultActivity::class.java).also {
            it.putExtras(bundle)
        }
        startActivity(intent)
        finish()
    }

    override fun onUploadFailed(uuid: String?, exception: Exception) {
        super.onUploadFailed(uuid, exception)
        val scannerInstance = TireTreadScanner.instance
        scannerInstance.stopScanning()

        mainHandler.post {
            Toast.makeText(applicationContext, "Upload Failed", Toast.LENGTH_SHORT).show()
        }
    }

    /***
     * Display the Distance Info (from the camera to tire)
     * anytime there is a change to it
     */
    override fun onDistanceChanged(
        uuid: String?,
        previousStatus: DistanceStatus,
        newStatus: DistanceStatus,
        previousDistance: Float,
        newDistance: Float,
    ) {
        super.onDistanceChanged(uuid, previousStatus, newStatus, previousDistance, newDistance)

        val distanceString =
            if (binding.tireTreadScanView.measurementSystem == MeasurementSystem.Imperial) {
                "${inchStringToTriple(inchToFractionString(newDistance.toDouble())).first} in"
            } else {
                "${(newDistance / 10).toInt()} cm"
            }

        var noticingMessage = ""
        var color = 0

        when (newStatus) {
            DistanceStatus.OK -> {
                color = ContextCompat.getColor(baseContext, R.color.distance_ok)
                noticingMessage = "Distance OK: $distanceString"
            }

            DistanceStatus.CLOSE -> {
                color = ContextCompat.getColor(baseContext, R.color.distance_far)
                noticingMessage = "Increase Distance: $distanceString"
            }

            DistanceStatus.TOO_CLOSE -> {
                color = ContextCompat.getColor(baseContext, R.color.distance_too_far)
                noticingMessage = "Increase Distance: $distanceString"
            }

            DistanceStatus.FAR -> {
                color = ContextCompat.getColor(baseContext, R.color.distance_far)
                noticingMessage = "Decrease Distance: $distanceString"
            }

            DistanceStatus.TOO_FAR -> {
                color = ContextCompat.getColor(baseContext, R.color.distance_too_far)
                noticingMessage = "Decrease Distance: $distanceString"
            }

            else -> {
                color = ContextCompat.getColor(baseContext, R.color.white)
                noticingMessage = String.format("Distance: $distanceString")
            }
        }

        mainHandler.post {
            binding.tvDistance.apply {
                text = noticingMessage
                setTextColor(color)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action: Int = event.action
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN && findViewById<Button>(R.id.btnStartScan).isClickable) {
                    findViewById<Button>(R.id.btnStartScan).performClick()
                }
                true
            }

            else -> super.dispatchKeyEvent(event)
        }
    }

    companion object {
        private const val MAX_DURATION_SCAN_SPEED_FAST_MILLIS = 7500L
        private const val MAX_DURATION_SCAN_SPEED_SLOW_MILLIS = 10000L
    }
}