package io.anyline.tiretread.devexample.results

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.Response
import io.anyline.tiretread.sdk.getHeatmap
import io.anyline.tiretread.sdk.getTreadDepthReportResult
import io.anyline.tiretread.sdk.types.Heatmap
import io.anyline.tiretread.sdk.types.TreadDepthResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and data fetching for Tread Depth Measurement results
 * and Heatmap results. This ViewModel uses a background dispatcher for asynchronous operations.
 *
 * @property backgroundDispatcher The CoroutineDispatcher used for background operations. Defaults to Dispatchers.IO.
 */

class ResultViewModel(
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val isBusy = mutableStateOf(false)
    val treadDepthResult = mutableStateOf<TreadDepthResult?>(null)
    val heatmap = mutableStateOf<Heatmap?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    /**
     * Asynchronously fetch Tread Depth Measurement results for the provided Measurement
     *
     * @param measurementUuid The UUID of the Measurement being requested
     */
    fun fetchResults(measurementUuid: String) {
        isBusy.value = true
        treadDepthResult.value = null
        errorMessage.value = null
        viewModelScope.launch(backgroundDispatcher) {
            AnylineTireTreadSdk.getTreadDepthReportResult(measurementUuid) {
                when (it) {
                    is Response.Success -> {
                        treadDepthResult.value = it.data
                    }

                    is Response.Error -> {
                        errorMessage.value = "${it.errorCode}: ${it.errorMessage}"
                    }

                    is Response.Exception -> {
                        errorMessage.value = it.exception.toString()
                    }
                }
                isBusy.value = false
            }
        }
    }

    /**
     * Asynchronously fetch the Heatmap result for the provided Measurement
     *
     * @param measurementUuid The UUID of the Measurement being requested
     */
    fun fetchHeatmap(measurementUuid: String) {
        isBusy.value = true
        heatmap.value = null
        errorMessage.value = null
        viewModelScope.launch(backgroundDispatcher) {
            AnylineTireTreadSdk.getHeatmap(measurementUuid) {
                when (it) {
                    is Response.Success -> {
                        heatmap.value = it.data
                    }

                    is Response.Error -> {
                        errorMessage.value =
                            "Error while loading the heatmap: ${it.errorCode} - ${it.errorMessage}"
                    }

                    is Response.Exception -> {
                        errorMessage.value = "Error while loading the heatmap: ${it.exception}"
                    }
                }
                isBusy.value = false
            }
        }
    }
}