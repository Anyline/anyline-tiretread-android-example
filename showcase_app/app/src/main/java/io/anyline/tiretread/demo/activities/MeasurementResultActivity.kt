package io.anyline.tiretread.demo.activities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.sdk.*
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import java.util.*

class MeasurementResultActivity : AppCompatActivity() {
    private var measurementUuid: String = ""
    private var currentResult = TreadDepthResult(
        TreadResultRegion(), listOf(TreadResultRegion())
    )

    lateinit var loadingView: View
    lateinit var tvStatus: TextView
    lateinit var btnOK: Button
    lateinit var btnDetails: Button
    lateinit var btnFeedback: Button

    private val resultTimer = Timer()

    private val resultUpdateTask = object : TimerTask() {
        override fun run() {
            var measurementFailed = false

            Log.d("SHOWCASE", "run: Checking for results for UUID - $measurementUuid - ...")

            val measurementResult = AnylineTireTreadSdk
                .getTreadDepthReportResult(
                    measurementUuid,
                    onGetTreadDepthReportResultFailed = { _, exception ->
                        // Handle failure
                        Log.e("SHOWCASE", "Completed with failure: " + exception.message)
                        measurementFailed = true
                    }
                )

            if (measurementFailed) {
                Log.d("SHOWCASE", "run: Error... cancel the loop now")
                Log.d("SHOWCASE", "UUID: $measurementUuid")
                resultTimer.cancel()

                runOnUiThread {
                    displayError(
                        "Make sure you do not move the device too fast and keep the right distance."
                    )
                }
            } else if (measurementResult != null) {
                Log.d("SHOWCASE", "run: Result not null... cancel the loop now")
                Log.d("SHOWCASE", "UUID: $measurementUuid")
                resultTimer.cancel()
                runOnUiThread {
                    displayMeasurementResult(measurementResult)
                }
                currentResult = measurementResult
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurement_result)

        val bundle = intent.extras
        bundle?.also {
            measurementUuid = it.getString("measurement_uuid") ?: ""
        }

        val txtResultToken = findViewById<TextView>(R.id.txtResultToken)
        txtResultToken.text = "${getString(R.string.txt_token)} $measurementUuid"

        txtResultToken.setOnClickListener {
            copyTextToClipboard(measurementUuid)
            Toast.makeText(this, "Scan ID copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        initLoadingScreen()

        resultTimer.schedule(resultUpdateTask, 0L, 3000L)
    }

    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHOWCASE", "run: View destroyed")
        resultTimer.cancel()
    }

    private fun initLoadingScreen() {
        loadingView = findViewById(R.id.gifResultLoadingAnimation)
        loadingView.visibility = View.VISIBLE

        tvStatus = findViewById(R.id.tvStatus)
        tvStatus.visibility = View.VISIBLE

        btnOK = findViewById(R.id.btnResultOk)
        btnOK.text = "Cancel"
        btnDetails = findViewById(R.id.btnResultReport)
        btnDetails.visibility = View.GONE
        btnFeedback = findViewById(R.id.btnResultFeedback)
        btnFeedback.visibility = View.GONE
    }

    private fun displayMeasurementResult(measurementResult: TreadDepthResult) {
        loadingView.visibility = View.GONE
        tvStatus.visibility = View.GONE

        btnOK.text = "Ok"
        btnDetails.visibility = View.VISIBLE
        btnFeedback.visibility = View.VISIBLE

        findViewById<LinearLayout>(R.id.llMeasurementResults).visibility = View.VISIBLE

        val llResultGlobal = findViewById<LinearLayout>(R.id.llResultGlobal)

        // Display the Global Result
        if (PreferencesUtils.shouldUseImperialSystem(this)) {

            val globalResultInch32nds = measurementResult.global.valueInch32nds

            // Display the background green/yellow/red according to the Tread Depth
            llResultGlobal.background = ContextCompat.getDrawable(
                this,
                getResultBackgroundDrawable(globalResultInch32nds)
            )

            findViewById<View>(R.id.dividerLineInches).visibility = View.VISIBLE

            findViewById<TextView>(R.id.tvResultInch32ndsGlobal).text =
                globalResultInch32nds.toString()
            findViewById<TextView>(R.id.tvDenominatorGlobal).visibility = View.VISIBLE
        } else {
            val globalResultMillimeter = measurementResult.global.valueMm
            // Display the background green/yellow/red according to the Tread Depth
            llResultGlobal.background = ContextCompat.getDrawable(
                this,
                getResultBackgroundDrawable(globalResultMillimeter)
            )

            val tvResultGlobal = findViewById<TextView>(R.id.tvResultGlobal)
            val globalString = String.format("%.1f", globalResultMillimeter) + "\nmm"
            tvResultGlobal.text = globalString
        }

        val llMeasurementResultRegions = findViewById<LinearLayout>(R.id.llMeasurementResultRegions)
        // Clean the region results
        llMeasurementResultRegions.removeAllViews()

        // Divide the layout by the total of regions and spaces around them.
        llMeasurementResultRegions.weightSum = (measurementResult.regions.size.toFloat() * 2)

        // Display the regions dynamically, from left to right.
        for (region in measurementResult.regions) {
            if (region.isAvailable)
                llMeasurementResultRegions.addView(createAvailableRegionResultView(region))
            else
                llMeasurementResultRegions.addView(createUnavailableRegionResultView())
        }
    }

    private fun displayError(message: String) {
        loadingView.visibility = View.GONE
        tvStatus.visibility = View.GONE
        btnOK.text = "Ok"

        val tvErrorTitle = findViewById<TextView>(R.id.tvErrorTitle)
        val tvErrorMessage = findViewById<TextView>(R.id.tvErrorMessage)

        tvErrorTitle.text = resources.getString(R.string.txt_error_title_result_activity)
        tvErrorMessage.text = message
        findViewById<LinearLayout>(R.id.llError).visibility = View.VISIBLE
    }

    private fun createAvailableRegionResultView(region: TreadResultRegion): View {
        val regionResultFragment =
            layoutInflater.inflate(R.layout.fragment_region_result, null, false)

        val llRegionResult =
            regionResultFragment.findViewById<LinearLayout>(R.id.llRegionResult)

        if (PreferencesUtils.shouldUseImperialSystem(this)) {
            llRegionResult.background =
                ContextCompat.getDrawable(this, getResultBackgroundDrawable(region.valueInch32nds))

            regionResultFragment.findViewById<View>(R.id.dividerLineInches).visibility =
                View.VISIBLE

            regionResultFragment.findViewById<TextView>(R.id.tvResult32ndsInch).text =
                region.valueInch32nds.toString()
            regionResultFragment
                .findViewById<TextView>(R.id.tvRegionResultDenominatorInch).visibility =
                View.VISIBLE
        } else {
            llRegionResult.background =
                ContextCompat.getDrawable(this, getResultBackgroundDrawable(region.valueMm))

            regionResultFragment.findViewById<TextView>(R.id.tvRegionResultMillimeter).text =
                String.format("%.1f", region.valueMm) + "\nmm"
        }
        return regionResultFragment
    }

    private fun createUnavailableRegionResultView(): View {
        val regionResultFragment =
            layoutInflater.inflate(R.layout.fragment_region_result, null, false)

        val tvRegionResult =
            regionResultFragment.findViewById<TextView>(R.id.tvRegionResultMillimeter)

        tvRegionResult.text = "-"
        tvRegionResult.background = ContextCompat.getDrawable(this, R.drawable.result_gray)

        return regionResultFragment
    }

    fun onClickedBtnFeedback(view: View) {
        val bundle = Bundle()
        bundle.putString("measurement_uuid", measurementUuid)
        bundle.putString("current_result", DefaultJson.encodeToString(currentResult))

        val intent = Intent(this, ResultFeedbackActivity::class.java).also {
            it.putExtras(bundle)
        }
        startActivity(intent)
    }

    /***
     * Display PDF report if available
     */
    fun onClickedBtnReport(view: View) {
        val uuid = measurementUuid
        val measurementResult = AnylineTireTreadSdk.getTreadDepthReportResult(uuid)
        val isResultReady = (measurementResult != null)

        if (!isResultReady) {
            Toast.makeText(
                this,
                "The detailed report cannot be displayed at the moment.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        startActivity(MeasurementResultDetailsActivity.newIntent(this, uuid))
    }

    fun finishActivity(view: View) {
        // remove loader callback handler
        resultTimer.cancel()

        // remove the current scan activity from the stack
        finishAndRemoveTask()
    }

    private fun getResultBackgroundDrawable(valueMillimeter: Double): Int {
        return if (valueMillimeter >= 4f) {
            R.drawable.result_green
        } else if (valueMillimeter <= 3f) {
            R.drawable.result_red
        } else {
            R.drawable.result_yellow
        }
    }

    private fun getResultBackgroundDrawable(valueInch32nds: Int): Int {
        return if (valueInch32nds > 5) {
            R.drawable.result_green
        } else if (valueInch32nds <= 3) {
            R.drawable.result_red
        } else {
            R.drawable.result_yellow
        }
    }
}