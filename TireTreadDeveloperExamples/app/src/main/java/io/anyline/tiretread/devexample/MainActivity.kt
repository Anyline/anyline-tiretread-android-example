package io.anyline.tiretread.devexample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import io.anyline.tiretread.devexample.results.ResultActivity
import io.anyline.tiretread.devexample.scan_process.ComposeScanActivity
import io.anyline.tiretread.devexample.scan_process.XmlScanActivity
import io.anyline.tiretread.devexample.ucr.UcrActivity
import io.anyline.tiretread.devexample.ui.components.DevExButton
import io.anyline.tiretread.devexample.ui.components.DevExErrorContent
import io.anyline.tiretread.devexample.ui.components.DevExLoadingView
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme
import io.anyline.tiretread.sdk.AnylineTireTreadSdk

/**
 * MainActivity is the entry point of the application, responsible for initializing the Anyline Tire Tread SDK
 * and providing navigation to different examples of the scan-process and result display.
 *
 * @property viewModel The ViewModel that manages the state and initialization of the Anyline Tire Tread SDK.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Called when the activity is starting. This is where most initialization should go.
     * Checks if the Anyline Tire Tread SDK is initialized, and if not, initializes it.
     * Sets up the content view with Compose.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if the TTR SDK is initialized
        if (!AnylineTireTreadSdk.isInitialized) {
            // if not, initialize it
            viewModel.initializeAnylineSDK(BuildConfig.LICENSE_KEY, this)
        }

        setContent {
            // Use you own app's theme, and define any UI elements as preferred.
            TTRDeveloperExamplesTheme {
                ScaffoldAndTopBar(content = {
                    ExampleApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(it)
                    )
                })
            }
        }
    }
}

/* Composable functions */

/**
 * Sets up the main content of the app, including buttons for different examples
 * and displaying error messages if the SDK initialization fails.
 *
 * @param viewModel The ViewModel that manages the state and initialization of the Anyline Tire Tread SDK.
 * @param modifier Modifier to be applied to the layout.
 */
@Composable
fun ExampleApp(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isBusy by viewModel.busy
    val ttrSdkInitialized by viewModel.ttrSdkInitialized
    val initializationErrorMessage by viewModel.tireTreadSdkInitializationErrorMessage

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        when {
            // Display error message if SDK initialization fails
            initializationErrorMessage.isNotEmpty() -> DevExErrorContent(initializationErrorMessage)
            // Buttons for different examples
            else -> ExampleButtons(ttrSdkInitialized)
        }

        // Display TTR SDK version, for debugging purposes
        Text(
            text = "SDK Version: ${AnylineTireTreadSdk.sdkVersion}",
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    // Display a custom loading indicator on top of the whole screen
    DevExLoadingView(isBusy)
}

/**
 * Sets up the buttons for different examples.
 *
 * @param ttrSdkInitialized Boolean indicating if the SDK is initialized.
 */
@Composable
fun ExampleButtons(ttrSdkInitialized: Boolean) {
    val context = LocalContext.current

    /* Scan Process */

    // Basic example, using a Compose Activity to scan
    DevExButton(text = R.string.default_config_compose, isEnabled = ttrSdkInitialized) {
        context.startActivity(Intent(context, ComposeScanActivity::class.java))
    }
    // Basic example, using an XML Activity to scan
    DevExButton(text = R.string.default_config_xml, isEnabled = ttrSdkInitialized) {
        context.startActivity(Intent(context, XmlScanActivity::class.java))
    }

    /* Results */

    // Example Measurement UUID for requesting results
    // ⚠️ In your implementation, use the Measurement UUID returned by
    // the 'onScanProcessCompleted' callback of the ScanView to request its results.
    val exampleMeasurementUUID = "8f2b96bc-8f0a-4a0a-8bbd-92f39270a0e7"

    // Basic example, requesting results for a specific example Measurement UUID
    DevExButton(text = R.string.results, isEnabled = ttrSdkInitialized) {
        context.startActivity(
            Intent(context, ResultActivity::class.java).apply {
                putExtra("measurementUUID", exampleMeasurementUUID)
            }
        )
    }

    /* UCR */

    // Basic example, requesting results for a specific example Measurement UUID
    DevExButton(text = R.string.ucr, isEnabled = ttrSdkInitialized) {
        context.startActivity(
            Intent(context, UcrActivity::class.java).apply {
                putExtra("measurementUUID", exampleMeasurementUUID)
            }
        )
    }
}

/**
 * Sets up the Scaffold with a top app bar.
 *
 * @param content The content to be displayed inside the Scaffold.
 * @param modifier Modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldAndTopBar(content: @Composable (PaddingValues) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TireTread Developer Examples",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

/* Previews */

@PreviewLightDark
@PreviewScreenSizes
@Composable
fun ExampleAppPreview() {
    val vm = MainViewModel().apply {
        ttrSdkInitialized.value = true
    }
    TTRDeveloperExamplesTheme {
        ScaffoldAndTopBar(content = {
            ExampleApp(
                viewModel = vm,
                modifier = Modifier.padding(it)
            )
        })
    }
}
