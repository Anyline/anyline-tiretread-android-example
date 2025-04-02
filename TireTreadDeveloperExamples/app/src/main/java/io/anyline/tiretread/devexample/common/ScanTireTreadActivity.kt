package io.anyline.tiretread.devexample.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.anyline.tiretread.devexample.databinding.ActivityScanTireTreadBinding
import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.scanner.ScanSpeed
import io.anyline.tiretread.sdk.scanner.TireTreadScanViewConfig
import io.anyline.tiretread.sdk.scanner.TireTreadScanner
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScanTireTreadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanTireTreadBinding
    private val scanTireTreadViewModel: ScanTireTreadViewModel by lazy {
        ViewModelProvider(this)[ScanTireTreadViewModel::class.java]
    }

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                scanTireTreadViewModel.measurementScanStateLiveData.value?.let {
                    val confirmationToAbortRequired = it.confirmationToAbortRequired()
                    when (confirmationToAbortRequired) {
                        is ScanTireTreadViewModel.MeasurementScanState.ConfirmationToAbortRequired.Yes -> {
                            if (doubleBackToExitPressedOnce) {
                                finishWithResult(
                                    RESULT_CANCELED, null, confirmationToAbortRequired.abortMessage
                                )
                                return@let
                            }
                            doubleBackToExitPressedOnce = true
                            Toast.makeText(
                                this@ScanTireTreadActivity,
                                "${confirmationToAbortRequired.confirmationMessage} Press again to abort.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Handler(Looper.getMainLooper()).postDelayed(
                                { doubleBackToExitPressedOnce = false }, 2000
                            )
                        }

                        is ScanTireTreadViewModel.MeasurementScanState.ConfirmationToAbortRequired.No -> {
                            finishWithResult(RESULT_CANCELED, null, CANCEL_MESSAGE_ABORTED)
                        }
                    }
                } ?: run { finish() }
            }
        })

        inflateLayout()

        scanTireTreadViewModel.resultAction.observe(this) {
            finishWithResult(it.first, it.second, it.third)
        }
    }

    private fun inflateLayout() {
        binding = ActivityScanTireTreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observeMeasurementScanState()
    }

    private fun observeMeasurementScanState() {
        scanTireTreadViewModel.measurementScanStateLiveData.observe(this) { measurementScanState ->
            with(binding) {
                when (measurementScanState.measurementResultStatus) {
                    null -> {
                        if (intent.getBooleanExtra(INTENT_EXTRA_IN_UPDATE_INTERFACE, false)) {
                            measurementResultUpdateInterface?.let {
                                scanTireTreadViewModel.measurementResultUpdateInterface = it
                            }
                            //once set listener to viewModel, release static measurementResultUpdateInterface reference
                            measurementResultUpdateInterface = null
                        }

                        val activityParameters: ScanTireTreadActivityParameters =
                            intent.getStringExtra(INTENT_EXTRA_IN_SCAN_TTD_ACTIVITY_PARAMETERS)
                                ?.let {
                                    ScanTireTreadActivityParameters.fromString(it)
                                } ?: run { ScanTireTreadActivityParameters() }

                        scanTireTreadViewModel.customDataContent = activityParameters.customData
                        scanTireTreadViewModel.scopeStrategy = activityParameters.scopeStrategy

                        var optionalTireWidth: Int? = null
                        binding.tireTreadScanView.apply {
                            val scanConfig = activityParameters.configContent?.let {
                                Json.decodeFromString<TireTreadScanViewConfig>(it)
                            } ?: run { TireTreadScanViewConfig() }

                            activityParameters.scanSpeed?.let {
                                scanConfig.scanSpeed = it
                            }

                            activityParameters.measurementSystem?.let {
                                scanConfig.measurementSystem = it
                            }

                            activityParameters.tireWidth?.let {
                                optionalTireWidth = it
                            }

                            activityParameters.showGuidance?.let {
                                scanConfig.defaultUiConfig.countdownConfig.visible = it
                                scanConfig.defaultUiConfig.scanDirectionConfig.visible = it
                                scanConfig.defaultUiConfig.tireOverlayConfig.visible = it
                            }
                            scanTireTreadViewModel.useDefaultUi = scanConfig.useDefaultUi

                            scanTireTreadViewModel.measurementSystem = scanConfig.measurementSystem
                            this.init(
                                tireTreadScanViewConfig = scanConfig,
                                tireWidth = optionalTireWidth,
                                onScanAborted = scanTireTreadViewModel::onScanAborted,
                                onScanProcessCompleted = scanTireTreadViewModel::onUploadCompleted,
                                onError = scanTireTreadViewModel::onError,
                                tireTreadScanViewCallback = scanTireTreadViewModel::handleScanEvent
                            )
                        }

                        processingLayout.visibility = View.GONE
                        tireTreadScanView.visibility = View.VISIBLE
                    }

                    is MeasurementResultStatus.ImageUploaded -> {
                        if (!scanTireTreadViewModel.useDefaultUi) {
                            processingProgressBar.isIndeterminate = false
                            processingProgressBar.max =
                                measurementScanState.measurementResultStatus.total
                            processingProgressBar.progress =
                                measurementScanState.measurementResultStatus.uploaded
                            processingLayout.visibility = View.VISIBLE
                            tireTreadScanView.visibility = View.GONE
                            processingTextView.text =
                                measurementScanState.measurementResultStatus.statusDescription
                        }
                    }

                    is MeasurementResultStatus.UploadCompleted, is MeasurementResultStatus.TreadDepthResultQueried -> {
                        processingProgressBar.isIndeterminate = true
                        processingLayout.visibility = View.VISIBLE
                        tireTreadScanView.visibility = View.GONE
                        processingTextView.text =
                            measurementScanState.measurementResultStatus.statusDescription
                    }

                    else -> {
                        processingLayout.visibility = View.GONE
                        tireTreadScanView.visibility = View.VISIBLE
                        processingTextView.text = ""
                    }
                }
            }
        }
    }

    override fun onStop() {
        if (TireTreadScanner.isInitialized) {
            TireTreadScanner.instance.apply {
                if (isScanning) {
                    stopScanning()
                }
            }
        }
        super.onStop()
    }

    private fun finishWithResult(
        result: Int, measurementResultData: MeasurementResultData?, cancelMessage: String? = null
    ) {

        val resultIntent = Intent()
        resultIntent.putExtra(
            INTENT_EXTRA_OUT_MEASUREMENT_RESULT_DATA, measurementResultData?.toString()
        )
        resultIntent.putExtra(
            INTENT_EXTRA_OUT_CUSTOM_DATA, scanTireTreadViewModel.customDataContent
        )
        resultIntent.putExtra(INTENT_EXTRA_OUT_CANCEL_MESSAGE, cancelMessage)
        setResult(result, resultIntent)
        finish()
    }

    @Serializable
    data class ScanTireTreadActivityParameters @JvmOverloads constructor(
        val configContent: String? = null,
        val scanSpeed: ScanSpeed? = null,
        val measurementSystem: MeasurementSystem? = null,
        val tireWidth: Int? = null,
        val showGuidance: Boolean? = null,
        val customData: String? = null,
        val scopeStrategy: ScanTireTreadViewModel.ScopeStrategy = ScanTireTreadViewModel.ScopeStrategy.WaitForProcessing
    ) {
        override fun toString() = Json.encodeToString(this)

        companion object {
            @JvmStatic
            fun fromString(value: String) =
                Json.decodeFromString<ScanTireTreadActivityParameters>(value)
        }
    }

    companion object {
        private const val INTENT_EXTRA_IN_SCAN_TTD_ACTIVITY_PARAMETERS =
            "INTENT_EXTRA_IN_SCAN_TTD_ACTIVITY_PARAMETERS"
        private const val INTENT_EXTRA_IN_UPDATE_INTERFACE = "INTENT_EXTRA_IN_UPDATE_INTERFACE"

        const val INTENT_EXTRA_OUT_MEASUREMENT_RESULT_DATA =
            "INTENT_EXTRA_OUT_MEASUREMENT_RESULT_DATA"
        const val INTENT_EXTRA_OUT_CUSTOM_DATA = "INTENT_EXTRA_OUT_CUSTOM_DATA"
        const val INTENT_EXTRA_OUT_CANCEL_MESSAGE = "INTENT_EXTRA_OUT_CANCEL_MESSAGE"

        const val CANCEL_MESSAGE_ABORTED = "Aborted."
        const val CANCEL_MESSAGE_ABORTED_WHILE_SCANNING = "Aborted while scanning."
        const val CANCEL_MESSAGE_ABORTED_WHILE_WAITING_FOR_RESULT =
            "Aborted while waiting for result."

        private var measurementResultUpdateInterface: MeasurementResultUpdateInterface? = null

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            scanTireTreadActivityParameters: ScanTireTreadActivityParameters,
            measurementResultUpdateInterface: MeasurementResultUpdateInterface? = null
        ): Intent {

            this.measurementResultUpdateInterface = measurementResultUpdateInterface

            val intent = Intent(context, ScanTireTreadActivity::class.java)
            intent.putExtra(
                INTENT_EXTRA_IN_SCAN_TTD_ACTIVITY_PARAMETERS,
                scanTireTreadActivityParameters.toString()
            )
            intent.putExtra(
                INTENT_EXTRA_IN_UPDATE_INTERFACE, (measurementResultUpdateInterface != null)
            )

            return intent
        }
    }
}
