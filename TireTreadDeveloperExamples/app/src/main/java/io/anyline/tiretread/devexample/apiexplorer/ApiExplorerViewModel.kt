package io.anyline.tiretread.devexample.apiexplorer

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.anyline.tiretread.sdk.api.AnylineTireTread
import io.anyline.tiretread.sdk.api.SdkResult
import io.anyline.tiretread.sdk.types.TreadDepthResult

class ApiExplorerViewModel : ViewModel() {

    // Init
    val isInitBusy = mutableStateOf(false)
    val isInitialized = mutableStateOf(false)
    val initError = mutableStateOf("")

    // Scan outcome
    val scanStatus = mutableStateOf("")
    val scanStatusIsError = mutableStateOf(false)

    // Results
    val isResultsBusy = mutableStateOf(false)
    val treadDepthResult = mutableStateOf<TreadDepthResult?>(null)
    val resultsStatus = mutableStateOf("")
    val resultsIsError = mutableStateOf(false)

    fun initializeSDK(licenseKey: String, context: Context) {
        isInitBusy.value = true
        initError.value = ""
        AnylineTireTread.initialize(context, licenseKey) { result ->
            when (result) {
                is SdkResult.Ok -> {
                    isInitialized.value = true
                }
                is SdkResult.Err -> {
                    initError.value = "${result.error.code}: ${result.error.message}"
                }
            }
            isInitBusy.value = false
        }
    }

    fun clearScanOutcome() {
        scanStatus.value = ""
        scanStatusIsError.value = false
    }

    fun setScanCompleted(uuid: String) {
        scanStatus.value = "Outcome: success ($uuid)"
        scanStatusIsError.value = false
    }

    fun setScanAborted() {
        scanStatus.value = "Outcome: aborted"
        scanStatusIsError.value = false
    }

    fun setScanFailed(message: String) {
        scanStatus.value = "Outcome: failed ($message)"
        scanStatusIsError.value = true
    }

    fun fetchResults(uuid: String) {
        isResultsBusy.value = true
        treadDepthResult.value = null
        resultsStatus.value = ""
        AnylineTireTread.getResult(uuid) { result ->
            when (result) {
                is SdkResult.Ok -> {
                    treadDepthResult.value = result.result
                    resultsStatus.value = "Results loaded"
                    resultsIsError.value = false
                }
                is SdkResult.Err -> {
                    resultsStatus.value = "${result.error.code}: ${result.error.message}"
                    resultsIsError.value = true
                }
            }
            isResultsBusy.value = false
        }
    }
}
