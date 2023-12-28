package io.anyline.tiretread.demo

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions.ZoomCallback
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeScanner(
    zoomCallback: ZoomCallback
) {

    private val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
        Barcode.FORMAT_QR_CODE
    ).setZoomSuggestionOptions(
        ZoomSuggestionOptions.Builder(zoomCallback).setMaxSupportedZoomRatio(2f).build()
    ).build()

    @OptIn(ExperimentalGetImage::class)
    fun scanBarcode(
        imageProxy: ImageProxy, onCompleteListener: (String?) -> Unit
    ) {
        imageProxy.image?.let {
            val image = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)

            var barcode: String? = null

            BarcodeScanning.getClient(options).process(image).addOnSuccessListener { barcodes ->
                barcode = barcodes.firstOrNull()?.rawValue
            }.addOnCompleteListener {
                imageProxy.close()
                onCompleteListener(barcode)
            }
        }
    }
}