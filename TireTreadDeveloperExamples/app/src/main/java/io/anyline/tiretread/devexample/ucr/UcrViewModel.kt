package io.anyline.tiretread.devexample.ucr

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.Response
import io.anyline.tiretread.sdk.getTreadDepthReportResult
import io.anyline.tiretread.sdk.sendCommentFeedback
import io.anyline.tiretread.sdk.sendTreadDepthResultFeedback
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and data fetching for Tread Depth Measurement results
 * and user-corrected results. This ViewModel uses a background dispatcher for asynchronous operations.
 *
 * @property backgroundDispatcher The CoroutineDispatcher used for background operations. Defaults to Dispatchers.IO.
 */

class UcrViewModel(
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val isBusy = mutableStateOf(false)
    val treadDepthResult = mutableStateOf<TreadDepthResult?>(null)
    val resultFetchErrorMessage = mutableStateOf<String?>(null)

    val feedbackResponse = mutableStateOf("")

    /**
     * Asynchronously send user-corrected results
     *
     * @param measurementUuid The UUID of the Measurement being corrected
     */
    fun sendResultFeedback(measurementUuid: String, regions: List<TreadResultRegion>) {
        isBusy.value = true
        feedbackResponse.value = ""
        viewModelScope.launch(backgroundDispatcher) {
            AnylineTireTreadSdk.sendTreadDepthResultFeedback(
                measurementUuid,
                regions
            ) {
                when (it) {
                    is Response.Success -> feedbackResponse.value = "Success"
                    is Response.Error -> feedbackResponse.value = "Error: ${it.errorMessage}"
                    is Response.Exception -> feedbackResponse.value = "An exception occurred.\n" +
                            "Make sure no corrected result was already sent for this measurement."
                }
                isBusy.value = false
            }
        }
    }

    /**
     * Asynchronously send user-corrected results
     *
     * @param measurementUuid The UUID of the Measurement being corrected
     */
    fun sendCommentFeedback(measurementUuid: String, comment: String) {
        isBusy.value = true
        feedbackResponse.value = ""
        viewModelScope.launch(backgroundDispatcher) {
            AnylineTireTreadSdk.sendCommentFeedback(
                measurementUuid,
                comment
            ) {
                when (it) {
                    is Response.Success -> feedbackResponse.value = "Success"
                    is Response.Error -> feedbackResponse.value = "Error: ${it.errorMessage}"
                    is Response.Exception -> feedbackResponse.value = "Exception ${it.exception}"
                }
                isBusy.value = false
            }
        }
    }

    /**
     * Asynchronously fetch Tread Depth Measurement results for the provided Measurement
     *
     * @param measurementUuid The UUID of the Measurement being requested
     */
    fun fetchResults(measurementUuid: String) {
        isBusy.value = true
        treadDepthResult.value = null
        resultFetchErrorMessage.value = null
        viewModelScope.launch(backgroundDispatcher) {
            AnylineTireTreadSdk.getTreadDepthReportResult(measurementUuid) {
                when (it) {
                    is Response.Success -> {
                        treadDepthResult.value = it.data
                    }

                    is Response.Error -> {
                        resultFetchErrorMessage.value = "${it.errorCode}: ${it.errorMessage}"
                    }

                    is Response.Exception -> {
                        resultFetchErrorMessage.value = it.exception.toString()
                    }
                }
                isBusy.value = false
            }
        }
    }
}
