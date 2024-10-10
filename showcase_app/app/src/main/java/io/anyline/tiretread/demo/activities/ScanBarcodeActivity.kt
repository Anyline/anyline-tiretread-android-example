package io.anyline.tiretread.demo.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import io.anyline.tiretread.demo.BarcodeScanner
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.ui.AnylineButton
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.Response
import io.anyline.tiretread.sdk.sendTireIdFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch

class ScanBarcodeActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var camera: Camera
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var loading: MutableState<Boolean>
    private lateinit var isScanning: MutableState<Boolean>
    private val imageAnalysis =
        ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        barcodeScanner = BarcodeScanner { p0 -> zoomIn(p0) }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        })
        setContent { Content() }
    }

    private fun zoomIn(zoomRatio: Float): Boolean {
        camera.cameraControl.setZoomRatio(zoomRatio)
        return true
    }

    @Composable
    private fun Content() {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {

            val (cameraPreview, abortButton, loadingIndicator, background, guidanceText) = createRefs()
            loading = remember { mutableStateOf(false) }
            isScanning = remember { mutableStateOf(true) }

            CameraPreview(modifier = Modifier.constrainAs(cameraPreview) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                height = Dimension.fillToConstraints
                width = Dimension.fillToConstraints
            })

            AnylineButton(context = this@ScanBarcodeActivity,
                text = "Abort",
                onClick = { finish() },
                modifier = Modifier.constrainAs(abortButton) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top, margin = 10.dp)
                    height = Dimension.wrapContent
                    width = Dimension.wrapContent
                })

            if (loading.value) {
                CircularProgressIndicator(modifier = Modifier.constrainAs(loadingIndicator) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    height = Dimension.wrapContent
                    width = Dimension.wrapContent
                })
            }

            if (!isScanning.value) {
                Text(text = "", modifier = Modifier
                    .constrainAs(background) {
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        height = Dimension.matchParent
                        width = Dimension.matchParent
                    }
                    .background(Color.White))
            }

            intent.getStringExtra(EXTRA_GUIDANCE_TEXT)?.let {
                val color = getColor(R.color.primary_anyline)

                Text(text = it,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier
                        .constrainAs(guidanceText) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom, margin = 24.dp)
                            start.linkTo(parent.start)
                            height = Dimension.wrapContent
                            width = Dimension.wrapContent
                        }
                        .background(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(color.red, color.green, color.blue, color.alpha)
                        )
                        .padding(16.dp))
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    @Composable
    fun CameraPreview(modifier: Modifier) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {

                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(surfaceProvider)

                    attachAnalyser()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        camera = cameraProvider.bindToLifecycle(
                            this@ScanBarcodeActivity as LifecycleOwner,
                            CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build(),
                            preview,
                            imageAnalysis
                        ).apply {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }, ContextCompat.getMainExecutor(this@ScanBarcodeActivity))
                }
            }, modifier = modifier
        )
    }

    private fun attachAnalyser() {
        imageAnalysis.setAnalyzer(
            Dispatchers.IO.asExecutor()
        ) {
            analyseBarcode(it)
        }
    }

    private fun analyseBarcode(imageProxy: ImageProxy) {
        barcodeScanner.scanBarcode(imageProxy) {
            it?.let {
                isScanning.value = false
                if (intent.getBooleanExtra(EXTRA_RECORDER_USE_CASE, false)) {
                    val tireId = extractTireId(it)
                    // continues to scan if the tire id is not present in the QRCode
                    if (tireId == null) {
                        isScanning.value = true
                        return@scanBarcode
                    }
                    onSuccess(tireId)
                } else {
                    onSuccess(it)
                }
                imageAnalysis.clearAnalyzer()
            }
        }
    }

    private fun onSuccess(result: String) {
        if (intent.getBooleanExtra(EXTRA_RECORDER_USE_CASE, false)) {
            showAlertDialog(result, "Is the Tire ID correct?", result)
        } else {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_BARCODE_RESULT, result)
            })
            finish()
        }
    }

    private fun extractTireId(url: String): String? {
        val regex = Regex("""[?&]tire_id=([^&]+)""")
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }

    private fun sendTireId(tireId: String) {
        intent.getStringExtra(EXTRA_MEASUREMENT_UUID)?.let {

            loading.value = true
            lifecycleScope.launch(Dispatchers.IO) {

                AnylineTireTreadSdk.sendTireIdFeedback(
                    it, tireId
                ) {
                    when (it) {
                        is Response.Success -> {
                            PreferencesUtils.addNewTireRegistrationToCurrentLicenseKey(
                                this@ScanBarcodeActivity,
                                tireId
                            )
                            onSendTireFeedbackSuccess()
                            showRegistrationCountToast(tireId)
                            return@sendTireIdFeedback
                        }

                        is Response.Error -> {
                            Log.e("Demo", "${it.errorCode} - ${it.errorMessage}")
                            onSendTireFeedbackFailed(tireId)
                        }

                        is Response.Exception -> {
                            Log.e("Demo", "${it.exception}")
                            onSendTireFeedbackFailed(tireId)
                        }
                    }
                }
            }
        }
    }

    private fun onSendTireFeedbackSuccess() {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(
                this@ScanBarcodeActivity,
                "Your data recording has been successfully submitted.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun onSendTireFeedbackFailed(tireId: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            loading.value = false
            showAlertDialog(tireId, "Error!", "Failed to send tire ID.")
        }
    }

    private fun showAlertDialog(
        tireId: String, title: String, message: String
    ) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setOnCancelListener {
            isScanning.value = true
            attachAnalyser()
            it.dismiss()
        }.setPositiveButton("Submit") { _, _ -> sendTireId(tireId) }
            .setNegativeButton("Rescan Barcode") { dialog, _ ->
                isScanning.value = true
                attachAnalyser()
                dialog.dismiss()
            }.setNeutralButton("Abort") { _, _ -> finish() }.create().show()
    }

    /**
     * Shows the Registration Count for the provided tireId.
     *
     * @param tireId Tire Id for which the registration count will be displayed
     */
    private fun showRegistrationCountToast(tireId: String) {
        val tireRegistrationCount =
            PreferencesUtils.loadTireRegistrationCountForCurrentLicenseKey(this, tireId)

        runOnUiThread {
            Toast.makeText(
                this,
                "Tire Id: $tireId \nRegistries: $tireRegistrationCount",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {

        const val EXTRA_BARCODE_RESULT = "EXTRA_BARCODE_RESULT"
        private const val EXTRA_MEASUREMENT_UUID = "EXTRA_MEASUREMENT_UUID"
        private const val EXTRA_RECORDER_USE_CASE = "EXTRA_RECORDER_USE_CASE"
        private const val EXTRA_GUIDANCE_TEXT = "EXTRA_GUIDANCE_TEXT"

        fun newIntent(context: Context): Intent {
            return Intent(context, ScanBarcodeActivity::class.java).apply {
                putExtra(EXTRA_GUIDANCE_TEXT, "Scan the QR code of your license key.")
            }
        }

        fun newIntentForRecorder(context: Context, measurementUUID: String): Intent {
            return newIntent(context).apply {
                putExtra(EXTRA_RECORDER_USE_CASE, true)
                putExtra(EXTRA_MEASUREMENT_UUID, measurementUUID)
                putExtra(EXTRA_GUIDANCE_TEXT, "Scan the QR code of the tire you have just scanned.")
            }
        }
    }
}