package io.anyline.tiretread.demo

import android.os.Build
import java.util.*

/**
 * Functions to get the hardware information from the device.
 */
object HardwareInfo {

    val androidApiLevel: Int = Build.VERSION.SDK_INT;

    /**
     * Get the device name with the format: "Manufacturer Model".
     * @return the name of the device.
     */
    fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault())
                .startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}