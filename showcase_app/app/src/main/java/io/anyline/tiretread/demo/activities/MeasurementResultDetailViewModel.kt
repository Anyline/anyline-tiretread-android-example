package io.anyline.tiretread.demo.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyline.tiretread.demo.Response
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.getTreadDepthReportPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MeasurementResultDetailViewModel : ViewModel() {

    private val _pdfByteStream = MutableLiveData<Response<ByteArray>>()
    var pdfByteStream: LiveData<Response<ByteArray>> = _pdfByteStream

    private val _file = MutableLiveData<Response<File>>()
    var file: LiveData<Response<File>> = _file

    fun loadPdf(uuid: String) {
        _pdfByteStream.postValue(Response.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            AnylineTireTreadSdk.getTreadDepthReportPdf(uuid, { data ->
                if (data.isNotEmpty()) {
                    onPdfResponse(Response.Success(data))
                } else {
                    onPdfResponse(
                        Response.Error(
                            "Error! Byte array empty!"
                        )
                    )
                }
            }, { exception ->
                onPdfResponse(
                    Response.Error(
                        exception.message ?: "Failed to load PDF report!"
                    )
                )
            })

        }
    }

    fun saveToFile() {
        _file.postValue(Response.Loading())

        _pdfByteStream.value?.let { response ->
            when (response) {
                is Response.Success -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        val tmpPdfFile = File.createTempFile("tit_report-", ".pdf")

                        // Write the PDF data into it
                        val fos = FileOutputStream(tmpPdfFile)
                        fos.write(response.result)
                        fos.close()
                        onSavePdfResponse(Response.Success(tmpPdfFile))
                    }
                }

                else -> {
                    onPdfResponse(Response.Error("Error: No result available!"))
                }
            }
        } ?: onPdfResponse(Response.Error("Error: No result available!"))
    }

    private fun onPdfResponse(response: Response<ByteArray>) {
        viewModelScope.launch(Dispatchers.Main) {
            _pdfByteStream.postValue(response)
        }
    }

    private fun onSavePdfResponse(response: Response<File>) {
        viewModelScope.launch(Dispatchers.Main) {
            _file.postValue(response)
        }
    }
}