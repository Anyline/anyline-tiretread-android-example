package io.anyline.tiretread.devexample.simple

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.anyline.tiretread.devexample.common.MeasurementResultData

class SimpleMeasurementViewModel: ViewModel() {
    var requestedScan: Boolean = false
    val measurementResultDataLiveData: MutableLiveData<MeasurementResultData> = MutableLiveData()
    val measurementErrorLiveData: MutableLiveData<String> = MutableLiveData()
}