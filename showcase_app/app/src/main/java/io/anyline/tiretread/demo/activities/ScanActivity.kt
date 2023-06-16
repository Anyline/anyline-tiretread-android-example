package io.anyline.tiretread.demo.activities

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.sdk.scanner.*
import io.anyline.tiretread.sdk.utils.inchStringToTriple
import io.anyline.tiretread.sdk.utils.inchToFractionString
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class ScanActivity : AppCompatActivity(), TireTreadScanViewCallback {

    lateinit var mainHandler: Handler
    var mediaPlayer : MediaPlayer = MediaPlayer()
    private val scanTimer = Timer()

    companion object {
        private var currentActivity: AppCompatActivity? = null

        private val activities: ArrayList<AppCompatActivity> = arrayListOf()

        private fun finishAllAndRemoveTasks() {
            for (activity in activities) activity.finishAndRemoveTask()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Start UI update loop
        Handler(this.mainLooper).removeCallbacksAndMessages(null)
        mainHandler = Handler(this.mainLooper)

        if (currentActivity != this) {
            currentActivity?.finish()
            currentActivity = this
        }

        // Configure the TireTreadScanView
        val scanView = currentActivity!!.findViewById<TireTreadScanView>(R.id.tireTreadScanView)
        scanView.scanViewCallback = this
        scanView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(currentActivity!!))
        scanView.measurementSystem = if(PreferencesUtils.shouldUseImperialSystem(currentActivity!!)) {
            MeasurementSystem.Imperial
        }else{
            MeasurementSystem.Metric
        }

        // Overlay for the upload information
        currentActivity!!.findViewById<TextView>(R.id.tmpOverlay).visibility = View.GONE

        activities.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHOWCASE", "Activity Destroyed")
        activities.remove(this)
    }

    fun onClickedBtnStartScan(view: View) {
        val scannerInstance = TireTreadScanner.instance

        val btnStartScan = (view as Button)
        if (!scannerInstance.isScanning) {
            scannerInstance.startScanning()
            btnStartScan.text = "Stop"
        } else {
            btnStartScan.visibility = View.GONE
            btnStartScan.isEnabled = false
            btnStartScan.isClickable = false
            scannerInstance.stopScanning()
        }
    }

    override fun onScanStart(uuid: String?) {
        super.onScanStart(uuid)

        val pbProgress = currentActivity?.findViewById<ProgressBar>(R.id.pbProgress)
        if(pbProgress != null) {
            pbProgress.visibility = View.VISIBLE
            pbProgress.max = 10
            pbProgress.progress = 10

            scanTimer.schedule(1000, 1000) { pbProgress.progress -= 1 }
        }
    }

    override fun onScanStop(uuid: String?) {
        super.onScanStop(uuid)
        Log.d("SHOWCASE", "onScanStop: Scan stopped")
        scanTimer.cancel()

        val pbProgress = currentActivity?.findViewById<ProgressBar>(R.id.pbProgress)
        pbProgress?.progress = 0

        mainHandler.post {
            currentActivity?.apply {
                val startButton = findViewById<Button>(R.id.btnStartScan)
                val overlay = findViewById<TextView>(R.id.tmpOverlay)

                startButton?.visibility = View.GONE
                startButton?.isEnabled = false
                startButton?.isClickable = false

                overlay?.visibility = View.VISIBLE
            }
        }
    }

    override fun onUploadCompleted(uuid: String?) {
        super.onUploadCompleted(uuid)

        val bundle = Bundle()
        bundle.putString("measurement_uuid", uuid)

        val intent = Intent(this, MeasurementResultActivity::class.java).also {
            it.putExtras(bundle)
        }

        Log.d("SHOWCASE", "onUploadCompleted")
        // Start the result activity
        startActivity(intent)

        // remove the current scan activity from the stack
        finishAllAndRemoveTasks()
    }

    override fun onUploadFailed(uuid: String?, exception: Exception) {
        super.onUploadFailed(uuid, exception)
        val scannerInstance = TireTreadScanner.instance
        scannerInstance.stopScanning()

        Log.e("SHOWCASE", "onUploadFailed: ", exception)
        mainHandler.post {
            Toast.makeText(applicationContext, "Upload Failed", Toast.LENGTH_SHORT).show()
        }

        // remove the current scan activity from the stack
        finishAllAndRemoveTasks()
    }

    override fun onImageUploaded(uuid: String?, uploaded: Int, total: Int) {
        super.onImageUploaded(uuid, uploaded, total)

        // we will only display information about the upload once the scan process is finished
        if (TireTreadScanner.instance.isScanning) return

        val pbProgress = currentActivity?.findViewById<ProgressBar>(R.id.pbProgress)
        pbProgress?.visibility = View.VISIBLE

        val isUploading = uploaded < total
        if (isUploading) {
            mainHandler.post {
                pbProgress?.max = total
                pbProgress?.progress = uploaded
            }
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

        val scanView = currentActivity!!.findViewById<TireTreadScanView>(R.id.tireTreadScanView)
        val measurementSystem = scanView.measurementSystem
        val distanceString: String = if(measurementSystem == MeasurementSystem.Imperial){
            "${inchStringToTriple(inchToFractionString(newDistance.toDouble())).first} in"
        } else {
            "${(newDistance/10).toInt()} cm"
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
            val tvDistance = currentActivity?.findViewById<TextView>(R.id.tvDistance)
//            val indicator = currentActivity?.findViewById<LinearLayout>(R.id.distanceIndicator)
//
//            indicator?.apply {
//                this.background.setTint(color)
//            }
            tvDistance?.apply {
                text = noticingMessage
                setTextColor(color)
            }

            if (newStatus != previousStatus) {
                playAudioDistanceFeedback(newStatus)
            }
        }
    }

    private fun playAudioDistanceFeedback(distanceStatus: DistanceStatus) {
        mediaPlayer.stop()
        when (distanceStatus) {
            DistanceStatus.OK -> {
                mediaPlayer = MediaPlayer.create(baseContext, R.raw.distance_ok)
                mediaPlayer.start()
            }
            DistanceStatus.CLOSE, DistanceStatus.TOO_CLOSE -> {
                mediaPlayer = MediaPlayer.create(baseContext, R.raw.increase_distance)
                mediaPlayer.start()
            }
            DistanceStatus.FAR, DistanceStatus.TOO_FAR -> {
                mediaPlayer = MediaPlayer.create(baseContext, R.raw.decrease_distance)
                mediaPlayer.start()
            }
            else -> { }
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
}