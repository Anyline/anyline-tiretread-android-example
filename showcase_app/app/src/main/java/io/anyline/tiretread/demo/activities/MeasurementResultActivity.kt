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
import androidx.core.content.FileProvider
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.sdk.*
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import io.anyline.tiretread.sdk.utils.inchStringToTriple
import io.anyline.tiretread.sdk.utils.inchToFractionString
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
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

            if (measurementFailed){
                Log.d("SHOWCASE", "run: Error... cancel the loop now")
                Log.d("SHOWCASE", "UUID: $measurementUuid")
                resultTimer.cancel()

                runOnUiThread {
                    displayError(
                        "Make sure you do not move the device too fast and keep the right distance."
                    )
                }
            }
            else if (measurementResult != null) {
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

        findViewById<LinearLayout>(R.id.llResultGlobal).background =
            ContextCompat.getDrawable(
                this,
                getResultBackgroundDrawable(measurementResult.global.valueMm)
            )

        // Display the Global Result
        if (PreferencesUtils.shouldUseImperialSystem(this)) {
            findViewById<View>(R.id.dividerLineInches).visibility = View.VISIBLE

            val (_, nominator, denominator) =
                inchStringToTriple(inchToFractionString(measurementResult.global.valueInch))

            findViewById<TextView>(R.id.tvResultInchGlobal).text = "$nominator"
            val stringDenominator = "$denominator\"" // 32"
            findViewById<TextView>(R.id.tvDenominatorGlobal).text = stringDenominator
        } else {
            val tvResultGlobal = findViewById<TextView>(R.id.tvResultGlobal)
            val globalString = String.format("%.1f", measurementResult.global.valueMm) + "\nmm"
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

        val llRegionResult = regionResultFragment.findViewById<LinearLayout>(R.id.llRegionResult)
        llRegionResult.background =
            ContextCompat.getDrawable(this, getResultBackgroundDrawable(region.valueMm))

        if (PreferencesUtils.shouldUseImperialSystem(this)) {
            regionResultFragment.findViewById<View>(R.id.dividerLineInches).visibility =
                View.VISIBLE

            val (_, nominator, denominator) =
                inchStringToTriple(inchToFractionString(region.valueInch))

            regionResultFragment.findViewById<TextView>(R.id.tvResultNominatorInch).text =
                "$nominator"
            val denominatorString = "$denominator\""
            regionResultFragment.findViewById<TextView>(R.id.tvResultDenominatorInch).text =
                denominatorString
        } else {
            regionResultFragment.findViewById<TextView>(R.id.tvRegionResult).text =
                String.format("%.1f", region.valueMm) + "\nmm"
        }
        return regionResultFragment
    }

    private fun createUnavailableRegionResultView(): View {
        val regionResultFragment =
            layoutInflater.inflate(R.layout.fragment_region_result, null, false)

        val tvRegionResult = regionResultFragment.findViewById<TextView>(R.id.tvRegionResult)

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

        Toast.makeText(this, "Will open the PDF report", Toast.LENGTH_SHORT).show()

        val reportURL = AnylineTireTreadSdk.getTreadDepthReportUrlString(uuid)

        if (reportURL.isNotBlank()) {
            try {
                val data = AnylineTireTreadSdk.getTreadDepthReportPdf(uuid)
                // Create a temporary file
                val tmpPdfFile = File.createTempFile("tit_report-", ".pdf")

                // Write the PDF data into it
                val fos = FileOutputStream(tmpPdfFile)
                fos.write(data)
                fos.close()

                // Prepare to show
                val browserIntent = Intent(Intent.ACTION_VIEW)
                val uri = FileProvider.getUriForFile(
                    applicationContext,
                    "$packageName.provider",
                    tmpPdfFile
                )
                browserIntent.setDataAndType(uri, "application/pdf")
                browserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val chooser = Intent.createChooser(browserIntent, "Open with")
                chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                startActivity(chooser)
            } catch (e: Exception) {
                Log.e("SHOWCASE", "onClickedBtnReport: ", e)
            }
        } else {
            Toast.makeText(this, "The Detailed report is not available", Toast.LENGTH_LONG).show()
        }
    }

    fun finishActivity(view: View) {
        // remove loader callback handler
        resultTimer.cancel()

        // remove the current scan activity from the stack
        finishAndRemoveTask()
    }

    private fun getResultBackgroundDrawable(value: Double): Int {
        return if (value >= 4f) {
            R.drawable.result_green
        } else if (value <= 3f) {
            R.drawable.result_red
        } else {
            R.drawable.result_yellow
        }
    }
}