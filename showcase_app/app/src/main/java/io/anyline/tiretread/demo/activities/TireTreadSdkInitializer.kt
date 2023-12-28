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

object TireTreadSdkInitializer {

    fun initSdk(activity: Activity, licenseKey: String, onSuccess: () -> Unit = { }): Boolean {

        // Initialize the SDK
        try {
            AnylineTireTreadSdk.init(licenseKey, activity)
            onSuccess.invoke()
            setShouldRequestTireId(activity)
            return true
        } catch (e: SdkLicenseKeyInvalidException) {
            Toast.makeText(
                activity,
                activity.getString(R.string.txt_error_setup_failure_license_key),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: SdkInitializeFailedException) {
            Toast.makeText(
                activity, activity.getString(R.string.txt_error_setup_failure), Toast.LENGTH_LONG
            ).show()
        }
        return false
    }

    private fun setShouldRequestTireId(context: Context) {
        val shouldRequestTireId = AnylineTireTreadSdk.Companion.shouldRequestTireIdFeedback()
        PreferencesUtils.setShouldRequestTireId(context, shouldRequestTireId)
    }
}