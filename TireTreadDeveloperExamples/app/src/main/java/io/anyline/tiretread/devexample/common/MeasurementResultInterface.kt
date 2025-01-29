package io.anyline.tiretread.devexample.common

fun interface MeasurementResultUpdateInterface {
    fun onMeasurementResultDataStatusUpdate(
        measurementResultData: MeasurementResultData,
        measurementResultStatus: MeasurementResultStatus,
        customData: String?)
}