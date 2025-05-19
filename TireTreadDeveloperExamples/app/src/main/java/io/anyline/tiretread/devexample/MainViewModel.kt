package io.anyline.tiretread.devexample

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.NoConnectionException
import io.anyline.tiretread.sdk.SdkLicenseKeyForbiddenException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException
import io.anyline.tiretread.sdk.init
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    /**
     * State that holds a status to inform the user that some processing is happening
     */
    val busy = mutableStateOf(false)

    /**
     * State that holds the TTR SDK initialization status
     */
    val ttrSdkInitialized = mutableStateOf(AnylineTireTreadSdk.isInitialized)

    /**
     * State that holds any TTR SDK initialization error messages
     */
    val tireTreadSdkInitializationErrorMessage = mutableStateOf("")

    /**
     * Asynchronously initialize the Tire Tread SDK
     *
     * @param licenseKey Your TTR SDK License Key
     * @param context Your Application/Activity context
     */
    fun initializeAnylineSDK(licenseKey: String, context: Context) {
        var errorMessage = ""
        // Initialize the TTR SDK async, in the background dispatcher.
        viewModelScope.launch(backgroundDispatcher) {
            // Try/Catch the SDK initialization
            try {
                busy.value = true
                AnylineTireTreadSdk.init(licenseKey, context)
                ttrSdkInitialized.value = AnylineTireTreadSdk.isInitialized
            } catch (e: SdkLicenseKeyInvalidException) {
                errorMessage = "SdkLicenseKeyInvalidException\n\n${e.message}"
            } catch (e: SdkLicenseKeyForbiddenException) {
                errorMessage = "SdkLicenseKeyForbiddenException\n\n${e.message}"
            } catch (e: NoConnectionException) {
                errorMessage = "NoConnectionException\n\n${e.message}"
            } catch (e: Exception) {
                errorMessage = "Exception\n\n${e.message}"
            } finally {
                busy.value = false
                tireTreadSdkInitializationErrorMessage.value = errorMessage
            }
        }
    }
}