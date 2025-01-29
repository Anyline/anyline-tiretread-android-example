package io.anyline.tiretread.devexample

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

class ResultViewModel(
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val treadDepthResult = mutableStateOf<TreadDepthResult?>(null)
    val heatmap = mutableStateOf<Heatmap?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    fun fetchResults(measurementUuid: String) {
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
            }
        }
    }

    fun fetchHeatmap(measurementUuid: String) {
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
            }
        }
    }
}