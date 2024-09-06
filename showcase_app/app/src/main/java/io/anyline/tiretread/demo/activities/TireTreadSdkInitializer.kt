package io.anyline.tiretread.demo.activities

import android.app.Activity
import android.content.Context
import android.widget.Toast
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.SdkInitializeFailedException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException
import io.anyline.tiretread.sdk.shouldRequestTireIdFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TireTreadSdkInitializer {

    fun initSdk(activity: Activity, licenseKey: String, onSuccess: () -> Unit = { }): Boolean {

        // Initialize the SDK
        val error = try {
            AnylineTireTreadSdk.init(licenseKey, activity)
            onSuccess.invoke()
            setShouldRequestTireId(activity)
            return true
        } catch (e: SdkLicenseKeyInvalidException) {
            activity.getString(R.string.txt_error_setup_failure_license_key)
        } catch (e: SdkInitializeFailedException) {
            activity.getString(R.string.txt_error_setup_failure)
        }

        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                activity, error, Toast.LENGTH_LONG
            ).show()
        }
        return false
    }

    private fun setShouldRequestTireId(context: Context) {
        val shouldRequestTireId = AnylineTireTreadSdk.Companion.shouldRequestTireIdFeedback()
        PreferencesUtils.setShouldRequestTireId(context, shouldRequestTireId)
    }
}