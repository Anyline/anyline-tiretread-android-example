package io.anyline.tiretread.devexample.common

import android.util.Log
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyline.tiretread.devexample.common.ScanTireTreadActivity.Companion.CANCEL_MESSAGE_ABORTED_WHILE_SCANNING
import io.anyline.tiretread.devexample.common.ScanTireTreadActivity.Companion.CANCEL_MESSAGE_ABORTED_WHILE_WAITING_FOR_RESULT
import io.anyline.tiretread.sdk.scanner.DistanceStatus
import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.scanner.OnDistanceChanged
import io.anyline.tiretread.sdk.scanner.OnImageUploaded
import io.anyline.tiretread.sdk.scanner.OnScanStarted
import io.anyline.tiretread.sdk.scanner.OnScanStopped
import io.anyline.tiretread.sdk.scanner.ScanEvent
import io.anyline.tiretread.sdk.scanner.TireTreadScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScanTireTreadViewModel : ViewModel() {

    data class MeasurementScanState(
        val isScanning: Boolean, val measurementResultStatus: MeasurementResultStatus?
    ) {

        sealed class ConfirmationToAbortRequired {
            data object No : ConfirmationToAbortRequired()
            data class Yes(
                val confirmationMessage: String, val abortMessage: String
            ) : ConfirmationToAbortRequired()
        }

        fun confirmationToAbortRequired(): ConfirmationToAbortRequired {
            if (isScanning) {
                return ConfirmationToAbortRequired.Yes(
                    MeasurementResultStatus.ScanStarted.statusDescription,
                    CANCEL_MESSAGE_ABORTED_WHILE_SCANNING
                )
            }
            if (measurementResultStatus is MeasurementResultStatus.TreadDepthResultQueried) {
                if (measurementResultStatus.treadDepthResultStatus == TreadDepthResultStatus.NotYetAvailable) {
                    return ConfirmationToAbortRequired.Yes(
                        TreadDepthResultStatus.NotYetAvailable.statusDescription,
                        CANCEL_MESSAGE_ABORTED_WHILE_WAITING_FOR_RESULT
                    )
                }
            }
            return ConfirmationToAbortRequired.No
        }
    }

    val measurementScanStateLiveData: MutableLiveData<MeasurementScanState> =
        MutableLiveData(MeasurementScanState(false, null))

    var measurementResultUpdateInterface: MeasurementResultUpdateInterface? = null

    enum class CameraPermissionState {
        NotRequested, Granted, Denied
    }

    val cameraPermissionStateLiveData: MutableLiveData<CameraPermissionState> =
        MutableLiveData(CameraPermissionState.NotRequested)

    private var measurementResultData: MeasurementResultData? = null
    var measurementSystem = MeasurementSystem.Metric
    var useDefaultUi = false
    var scopeStrategy: ScopeStrategy = ScopeStrategy.WaitForProcessing
    var customDataContent: String? = null

    private val _resultAction: MutableLiveData<Triple<Int, MeasurementResultData?, String?>> =
        MutableLiveData()
    val resultAction: LiveData<Triple<Int, MeasurementResultData?, String?>> = _resultAction

    private fun provideNewMeasurementResultData(measurementUUID: String): MeasurementResultData {
        return MeasurementResultData(
            measurementUUID = measurementUUID, measurementSystem = measurementSystem
        )
    }

    fun handleScanEvent(event: ScanEvent) {
        when (event) {
            is OnScanStarted -> onScanStart(event.measurementUUID)
            is OnScanStopped -> onMeasurementResultDataStatusUpdate(MeasurementResultStatus.ScanStopped)
            is OnDistanceChanged -> onDistanceChanged(event.newStatus)
            is OnImageUploaded -> onMeasurementResultDataStatusUpdate(
                MeasurementResultStatus.ImageUploaded(
                    event.uploaded, event.total
                )
            )

            else -> {
                Log.i("ScanEvent", event.toString())
            }
        }
    }

    fun onScanAborted(measurementUUID: String?) {
        MeasurementResultStatus.ScanAborted.also { scanAbortedStatus ->
            onMeasurementResultDataStatusUpdate(scanAbortedStatus)
            _resultAction.postValue(
                Triple(
                    RESULT_CANCELED, measurementResultData, scanAbortedStatus.statusDescription
                )
            )
        }
    }

    fun onError(measurementUUID: String?, exception: Exception) {
        onMeasurementResultDataStatusUpdate(MeasurementResultStatus.Error(exception.message))
        _resultAction.postValue(Triple(RESULT_CANCELED, measurementResultData, exception.message))
    }

    private fun onScanStart(uuid: String?) {
        uuid?.let { measurementUUID ->
            measurementResultData = provideNewMeasurementResultData(measurementUUID)
            onMeasurementResultDataStatusUpdate(MeasurementResultStatus.ScanStarted)
        }
    }

    private fun onDistanceChanged(
        newStatus: DistanceStatus,
    ) {
        //when no defaultUI is used, start scanning when distance is ok
        if (newStatus == DistanceStatus.OK) {
            TireTreadScanner.instance.apply {
                if (!isScanning && !useDefaultUi) {
                    startScanning()
                }
            }
        }
    }

    fun onUploadCompleted(measurementUUID: String) {
        onMeasurementResultDataStatusUpdate(MeasurementResultStatus.UploadCompleted)
        when (scopeStrategy) {
            ScopeStrategy.WaitForProcessing -> {
                //fetch Tread Depth Report and wait for Result
                onMeasurementResultDataStatusUpdate(
                    MeasurementResultStatus.TreadDepthResultQueried(TreadDepthResultStatus.NotYetAvailable)
                )
                viewModelScope.launch(Dispatchers.IO) {
                    measurementResultData?.getTreadDepthReportResult { treadDepthResultStatus ->
                        onMeasurementResultDataStatusUpdate(
                            MeasurementResultStatus.TreadDepthResultQueried(
                                treadDepthResultStatus
                            )
                        )
                        viewModelScope.launch(Dispatchers.Main) {
                            _resultAction.postValue(
                                Triple(
                                    RESULT_OK, measurementResultData, null
                                )
                            )
                        }
                    }
                }
            }

            ScopeStrategy.CaptureAndUploadOnly -> {
                //Tread Depth Report Result will be fetch later/somewhere else
                _resultAction.postValue(Triple(RESULT_OK, measurementResultData, null))
            }
        }
    }

    private fun onMeasurementResultDataStatusUpdate(measurementResultStatus: MeasurementResultStatus) {
        measurementScanStateLiveData.postValue(
            MeasurementScanState(
                TireTreadScanner.instance.isScanning, measurementResultStatus
            )
        )
        measurementResultData?.let { measurementResultDataNonNull ->
            measurementResultDataNonNull.measurementResultStatus = measurementResultStatus

            measurementResultUpdateInterface?.onMeasurementResultDataStatusUpdate(
                measurementResultDataNonNull, measurementResultStatus, customDataContent
            )
        }
    }

    enum class ScopeStrategy {
        WaitForProcessing, CaptureAndUploadOnly
    }
}