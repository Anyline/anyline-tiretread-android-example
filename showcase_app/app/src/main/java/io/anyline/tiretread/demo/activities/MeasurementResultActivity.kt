package io.anyline.tiretread.demo.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.databinding.ActivityMeasurementResultBinding
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.getTreadDepthReportResult
import io.anyline.tiretread.sdk.getTreadDepthReportUrlString
import io.anyline.tiretread.sdk.types.MeasurementError
import io.anyline.tiretread.sdk.types.MeasurementInfo
import io.anyline.tiretread.sdk.types.MeasurementStatus
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.random.asKotlinRandom

class MeasurementResultActivity(
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppCompatActivity() {

    private var measurementUuid: String = ""
    private var currentResult = TreadDepthResult(
        TreadResultRegion(),
        listOf(TreadResultRegion()),
        MeasurementInfo(measurementUuid, MeasurementStatus.Unknown, null)
    )

    private lateinit var binding: ActivityMeasurementResultBinding

    private val titlesWithMessages = arrayOf(
        Pair("Fun fact!", "A tire check today, keeps you safe and on your way!"), Pair(
            "Did you know?", "Worn tires have less grip.\nReplace them when signs of wear appear!"
        ), Pair(
            "Did you know?",
            "Tires are designed for diverse driving conditions.\nTread patterns keep you safe in rain and snow!"
        ), Pair(
            "Fun fact!",
            "The world’s largest tire is over 24 meters tall.\nIt was used as a Ferris wheel in the 1964 World’s Fair!"
        ), Pair(
            "Fun fact!",
            "Lego is the world's largest tire producer.\nThey make over 300 million tires annually!"
        ), Pair(
            "Did you know?",
            "11,000 accidents per year are due to a bad tire.\nMake sure to check your tires regularly!"
        ), Pair(
            "Did you know?",
            "Tires are getting quieter.\nTread patterns play a key role in minimising road noise!"
        ), Pair(
            "Did you know?",
            "In the early 1900s, tires were grey or beige.\nAdding carbon made them change colour to black."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        measurementUuid = intent.extras?.getString("measurement_uuid") ?: ""

        with(binding.txtResultToken) {

            text = getString(R.string.txt_token, measurementUuid)
            setOnClickListener {
                copyTextToClipboard(measurementUuid)
                Toast.makeText(
                    this@MeasurementResultActivity,
                    "Scan ID copied to clipboard",
                    Toast.LENGTH_SHORT
                ).show()
            }
            initLoadingScreen()
        }

        lifecycleScope.launch(backgroundDispatcher) {
            fetchResults()
        }
    }

    private fun fetchResults() {
        AnylineTireTreadSdk.getTreadDepthReportResult(measurementUuid, { result: TreadDepthResult ->
            currentResult = result
            runOnUiThread {
                // TreadDepths can be retrieved in the 'result.global' and 'result.regions' properties.
                // more information about the measurement can be retrieved in 'result.measurementInfo' property.
                displayMeasurementResult(result)
            }
        }, { measurementError: MeasurementError ->
            runOnUiThread {
                displayError(measurementError.toString())
            }
        })
    }

    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun initLoadingScreen() {
        with(binding) {
            gifImageView.visibility = View.VISIBLE
            btnResultReport.visibility = View.GONE

            val randomNumber = Random().asKotlinRandom().nextInt(0, 7)
            loadingTitle.text = titlesWithMessages[randomNumber].first
            loadingMessage.text = titlesWithMessages[randomNumber].second
        }
    }

    private fun displayMeasurementResult(measurementResult: TreadDepthResult) {
        with(binding) {
            gifImageView.visibility = View.GONE
            btnResultReport.visibility = View.VISIBLE
            btnResultOk.visibility = View.VISIBLE
            cancelButton.visibility = View.GONE
            loadingViewHolder.visibility = View.GONE
            llMeasurementResults.visibility = View.VISIBLE

            // Display the Global Result
            if (PreferencesUtils.shouldUseImperialSystem(this@MeasurementResultActivity)) {

                val globalResultInch32nds = measurementResult.global.valueInch32nds

                // Display the background green/yellow/red according to the Tread Depth
                llResultGlobal.background = ContextCompat.getDrawable(
                    this@MeasurementResultActivity,
                    getResultBackgroundDrawable(globalResultInch32nds)
                )
                dividerLineInches.visibility = View.VISIBLE

                tvResultInch32ndsGlobal.text = globalResultInch32nds.toString()
                tvDenominatorGlobal.visibility = View.VISIBLE
            } else {
                val globalResultMillimeter = measurementResult.global.valueMm
                // Display the background green/yellow/red according to the Tread Depth
                llResultGlobal.background = ContextCompat.getDrawable(
                    this@MeasurementResultActivity,
                    getResultBackgroundDrawable(globalResultMillimeter)
                )
                tvResultGlobal.text = String.format("%.1f\nmm", globalResultMillimeter)
            }

            // Clean the region results
            llMeasurementResultRegions.removeAllViews()

            // Divide the layout by the total of regions and spaces around them.
            llMeasurementResultRegions.weightSum = (measurementResult.regions.size.toFloat() * 2)

            // Display the regions dynamically, from left to right.
            for (region in measurementResult.regions) {
                if (region.isAvailable) llMeasurementResultRegions.addView(
                    createAvailableRegionResultView(region)
                )
                else llMeasurementResultRegions.addView(createUnavailableRegionResultView())
            }
        }
    }

    private fun displayError(message: String) {
        with(binding) {
            gifImageView.visibility = View.GONE
            tvErrorTitle.text = resources.getString(R.string.txt_error_title_result_activity)
            tvErrorMessage.text = message
            errorScrollView.visibility = View.VISIBLE
            loadingViewHolder.visibility = View.GONE
            cancelButton.visibility = View.GONE
            btnResultOk.visibility = View.VISIBLE
        }
    }

    private fun createAvailableRegionResultView(region: TreadResultRegion): View {
        val regionResultFragment =
            layoutInflater.inflate(R.layout.fragment_region_result, null, false)

        val llRegionResult = regionResultFragment.findViewById<LinearLayout>(R.id.llRegionResult)

        if (PreferencesUtils.shouldUseImperialSystem(this)) {
            llRegionResult.background =
                ContextCompat.getDrawable(this, getResultBackgroundDrawable(region.valueInch32nds))

            regionResultFragment.findViewById<View>(R.id.dividerLineInches).visibility =
                View.VISIBLE

            regionResultFragment.findViewById<TextView>(R.id.tvResult32ndsInch).text =
                region.valueInch32nds.toString()
            regionResultFragment.findViewById<TextView>(R.id.tvRegionResultDenominatorInch).visibility =
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

    /***
     * Display PDF report if available
     */
    fun onClickedBtnReport(view: View) {
        val uuid = measurementUuid
        AnylineTireTreadSdk.getTreadDepthReportUrlString(uuid, {
            startActivity(MeasurementResultDetailsActivity.newIntent(this, uuid))
        }, { exception ->
            Log.e("Showcase", "Exception raised while loading the PDF", exception)
            Toast.makeText(
                this, "The detailed report cannot be displayed at the moment.", Toast.LENGTH_SHORT
            ).show()
        })
    }

    fun finishActivity(view: View) {
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