package io.anyline.tiretread.devexample.simple

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.anyline.tiretread.devexample.MeasurementResultDetailsFragment
import io.anyline.tiretread.devexample.MeasurementResultErrorFragment
import io.anyline.tiretread.devexample.MeasurementResultFragment
import io.anyline.tiretread.devexample.config.ValidationResult
import io.anyline.tiretread.devexample.common.MeasurementResultData
import io.anyline.tiretread.devexample.common.MeasurementResultData.Companion.fromString
import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import io.anyline.tiretread.devexample.common.ScanTireTreadActivity
import io.anyline.tiretread.devexample.common.TreadDepthResultStatus
import io.anyline.tiretread.devexample.common.encodeToString
import io.anyline.tiretread.devexample.databinding.ActivitySimpleMeasurementBinding
import io.anyline.tiretread.sdk.types.TreadDepthResult


class SimpleMeasurementActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySimpleMeasurementBinding
    private lateinit var measurementResultFragment: MeasurementResultFragment
    private lateinit var measurementResultDetailsFragment: MeasurementResultDetailsFragment
    private lateinit var measurementResultErrorFragment: MeasurementResultErrorFragment

    private val simpleMeasurementViewModel: SimpleMeasurementViewModel by lazy {
        ViewModelProvider(this)[SimpleMeasurementViewModel::class.java]
    }

    private val getScanTireTreadActivityResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val measurementResultDataString = it.data?.getStringExtra(
                    ScanTireTreadActivity.INTENT_EXTRA_OUT_MEASUREMENT_RESULT_DATA)

                if (measurementResultDataString != null) {
                    simpleMeasurementViewModel.measurementResultDataLiveData
                        .postValue(fromString(measurementResultDataString))
                }
            }
            else if (it.resultCode == RESULT_CANCELED) {
                it.data?.let { resultIntent ->
                    resultIntent.getStringExtra(
                        ScanTireTreadActivity.INTENT_EXTRA_OUT_CANCEL_MESSAGE)?.let { cancelMessage ->
                        simpleMeasurementViewModel.measurementErrorLiveData.postValue(cancelMessage)
                    }
                } ?: run {
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleMeasurementBinding.inflate(layoutInflater)
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

        simpleMeasurementViewModel.measurementResultDataLiveData.observe(this) { measurementResultData ->
            checkForResult(measurementResultData)
        }
        simpleMeasurementViewModel.measurementErrorLiveData.observe(this) { measurementErrorData ->
            showError("Error", measurementErrorData)
        }

        if (!simpleMeasurementViewModel.requestedScan) {
            intent.getStringExtra(INTENT_EXTRA_IN_SIMPLE_MEASUREMENT_ACTIVITY_CONFIG_CONTENT)?.let {
                val validatedConfig = ValidationResult.Succeed.fromString(it)
                requestTireTreadScan(validatedConfig)
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

    private fun requestTireTreadScan(
        selectConfigContent: ValidationResult.Succeed) {

        val intent = ScanTireTreadActivity.buildIntent(
            this,
            ScanTireTreadActivity.ScanTireTreadActivityParameters(
                configContent = selectConfigContent.configFileContent,
                scanSpeed = selectConfigContent.scanSpeed,
                measurementSystem = selectConfigContent.measurementSystem,
                tireWidth = selectConfigContent.tireWidth,
                showGuidance = selectConfigContent.showGuidance)
        )
        getScanTireTreadActivityResult.launch(intent)
        simpleMeasurementViewModel.requestedScan = true
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
        hideError()
        binding.mainResultLayout.visibility = View.VISIBLE
        binding.jsonResultEditText.setText(treadDepthResult.encodeToString())
        measurementResultFragment.displayMeasurementResult(
            measurementResultData,
            treadDepthResult)
        measurementResultDetailsFragment.measurementResultData = measurementResultData
        measurementResultErrorFragment.measurementResultStatus = measurementResultData.measurementResultStatus
    }

    private fun hideResult() {
        binding.mainResultLayout.visibility = View.GONE
    }

    private fun showError(title: String, message: String?, enableRetry: (() -> Unit)? = null) {
        hideResult()
        measurementResultErrorFragment.showError(title, message, enableRetry)
    }

    private fun hideError() {
        measurementResultErrorFragment.hideError()
    }

    companion object {
        private const val INTENT_EXTRA_IN_SIMPLE_MEASUREMENT_ACTIVITY_CONFIG_CONTENT =
            "INTENT_EXTRA_IN_SIMPLE_MEASUREMENT_ACTIVITY_CONFIG_CONTENT"
        fun buildIntent(context: Context, selectConfigContent: ValidationResult.Succeed): Intent {
            val intent = Intent(context, SimpleMeasurementActivity::class.java)
            intent.putExtra(INTENT_EXTRA_IN_SIMPLE_MEASUREMENT_ACTIVITY_CONFIG_CONTENT,
                selectConfigContent.toString())
            return intent
        }
    }
}

