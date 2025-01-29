package io.anyline.tiretread.devexample.advanced

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.devexample.MeasurementResultDetailsFragment
import io.anyline.tiretread.devexample.MeasurementResultErrorFragment
import io.anyline.tiretread.devexample.MeasurementResultFragment
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.common.MeasurementResultData
import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import io.anyline.tiretread.devexample.common.TreadDepthResultStatus
import io.anyline.tiretread.devexample.databinding.ActivityMeasurementResultBinding
import io.anyline.tiretread.sdk.types.TreadDepthResult

class MeasurementResultActivity() : AppCompatActivity() {
    private lateinit var measurementResultData: MeasurementResultData

    private lateinit var binding: ActivityMeasurementResultBinding
    private lateinit var measurementResultFragment: MeasurementResultFragment
    private lateinit var measurementResultDetailsFragment: MeasurementResultDetailsFragment
    private lateinit var measurementResultErrorFragment: MeasurementResultErrorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        measurementResultFragment = MeasurementResultFragment()
        measurementResultDetailsFragment = MeasurementResultDetailsFragment()
        measurementResultErrorFragment = MeasurementResultErrorFragment()

        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainerMeasurementResult.id, measurementResultFragment)
            .add(binding.fragmentContainerMeasurementResultDetails.id, measurementResultDetailsFragment)
            .add(binding.fragmentContainerMeasurementResultError.id, measurementResultErrorFragment)
            .commit()

        intent.extras?.getString(INTENT_EXTRA_IN_MEASUREMENT_RESULT_DATA)?.let { measurementResultDataString ->
            measurementResultData = MeasurementResultData.fromString(measurementResultDataString)
        } ?: {
            //TODO: Alert missing extra info
        }

        with(binding.txtResultToken) {
            text = getString(R.string.txt_token, measurementResultData.measurementUUID)
            setOnClickListener {
                copyTextToClipboard(measurementResultData.measurementUUID)
                Toast.makeText(
                    this@MeasurementResultActivity,
                    "Scan ID copied to clipboard",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        checkForResult(measurementResultData)
    }

    private fun checkForResult(measurementResultData: MeasurementResultData) {
        measurementResultErrorFragment.measurementResultStatus = measurementResultData.measurementResultStatus

        val measurementResultStatus = measurementResultData.measurementResultStatus
        if (measurementResultStatus is MeasurementResultStatus.TreadDepthResultQueried) {
            val treadDepthResultStatus = measurementResultStatus.treadDepthResultStatus
            if (treadDepthResultStatus is TreadDepthResultStatus.Succeed) {
                showResult(
                    measurementResultData,
                    treadDepthResultStatus.treadDepthResult)
                return
            }
        }
        hideResult()
    }

    private fun showResult(measurementResultData: MeasurementResultData, treadDepthResult: TreadDepthResult) {
        measurementResultFragment.displayMeasurementResult(
            measurementResultData,
            treadDepthResult)
        measurementResultDetailsFragment.measurementResultData = measurementResultData
    }

    private fun hideResult() {
        measurementResultFragment.hideMeasurementResult()
    }

    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    companion object {
        private const val TAG = "AnylineMeasurementResultActivity"
        private const val INTENT_EXTRA_IN_MEASUREMENT_RESULT_DATA = "INTENT_EXTRA_IN_MEASUREMENT_RESULT_DATA"

        fun buildIntent(context: Context, measurementResultData: MeasurementResultData): Intent {
            val intent = Intent(context, MeasurementResultActivity::class.java)
            intent.putExtra(INTENT_EXTRA_IN_MEASUREMENT_RESULT_DATA, measurementResultData.toString())
            return intent
        }
    }
}