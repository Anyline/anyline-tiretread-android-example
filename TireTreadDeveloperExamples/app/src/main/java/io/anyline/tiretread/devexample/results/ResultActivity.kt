package io.anyline.tiretread.devexample.results

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.anyline.tiretread.devexample.ui.components.DevExErrorContent
import io.anyline.tiretread.devexample.ui.components.DevExLoadingView
import io.anyline.tiretread.devexample.ui.components.DevExMeasurementSearch
import io.anyline.tiretread.devexample.ui.components.DevExMeasurementUnit
import io.anyline.tiretread.devexample.ui.components.DevExSectionHeader
import io.anyline.tiretread.devexample.ui.components.DevExUnitSelectionRadioGroup
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme
import io.anyline.tiretread.sdk.types.Heatmap
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import java.util.Locale

/**
 * Activity responsible for displaying the Tread Depth Measurement results and Heatmap.
 * This activity fetches the data based on a provided Measurement UUID and displays the results
 * using Jetpack Compose UI components.
 *
 * @property viewModel The ViewModel that manages the state and data fetching for this activity.
 */
class ResultActivity : ComponentActivity() {

    private val viewModel: ResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve the Measurement UUID from the intent
        val measurementUUID = intent.getStringExtra("measurementUUID")

        setContent {
            // Use you own app's theme, and define any UI elements as preferred.
            TTRDeveloperExamplesTheme {
                ResultScreen(viewModel, measurementUUID)
            }
        }
    }
}

/**
 * Displays the Tread Depth Measurement results and Heatmap.
 * It fetches the data based on a provided Measurement UUID and displays the results
 *
 * @param viewModel The ViewModel that manages the state and data fetching for this screen.
 * @param measurementUUID The UUID of the measurement to fetch data for.
 * @param modifier Modifier to be applied to the layout.
 */
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    measurementUUID: String?,
    modifier: Modifier = Modifier
) {
    val isBusy by viewModel.isBusy
    val treadDepthResult by viewModel.treadDepthResult
    val errorMessage by viewModel.errorMessage
    val heatmap by viewModel.heatmap

    var uuid by rememberSaveable { mutableStateOf(measurementUUID ?: "") }
    var shouldLoadResults by rememberSaveable { mutableStateOf(true) }

    // Effect to fetch results and heatmap when shouldLoadResults is true
    LaunchedEffect(shouldLoadResults) {
        if (shouldLoadResults) {
            // Fetch results and heatmap based on the UUID
            viewModel.fetchResults(uuid)
            viewModel.fetchHeatmap(uuid)
            shouldLoadResults = false
        }
    }

    // Main layout container
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        // Top app bar for the results screen
        ResultTopAppBar()

        // Allow searching for a specific Measurement UUID
        // this will probably not be required in your application
        val keyboardController = LocalSoftwareKeyboardController.current
        DevExMeasurementSearch(
            measurementUuid = uuid,
            onUuidChanged = { uuid = it.trim() },
            onDone = {
                shouldLoadResults = true
                keyboardController?.hide()
            })

        // Display content based on the vm state
        when {
            treadDepthResult != null -> ResultContent(treadDepthResult!!, heatmap)
            errorMessage != null -> DevExErrorContent(errorMessage!!)
        }
    }

    // Display a custom loading indicator on top of the whole screen
    DevExLoadingView(isBusy)
}

/**
 * Displays the Tread Depth Measurement results and Heatmap.
 * It includes unit selection, result views, and a heatmap.
 *
 * @param results The tread depth measurement results.
 * @param heatmap The heatmap data, if available.
 */
@Composable
fun ResultContent(results: TreadDepthResult, heatmap: Heatmap?, modifier: Modifier = Modifier) {
    var selectedMeasurementUnit by rememberSaveable { mutableStateOf(DevExMeasurementUnit.Mm) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.padding(bottom = 40.dp)
    ) {
        DevExSectionHeader("Results:")

        // Radio buttons, allowing the selection of the Measurement Unit to be displayed
        DevExUnitSelectionRadioGroup(selectedMeasurementUnit) { selectedMeasurementUnit = it }

        // Global and Minimum values
        ResultView(
            name = "Global value",
            result = results.global,
            measurementUnit = selectedMeasurementUnit
        )
        ResultView(
            name = "Minimum value",
            result = results.minimumValue,
            measurementUnit = selectedMeasurementUnit
        )

        // Show the Heatmap, whenever available
        heatmap?.url?.let { HeatMapView(it) }

        // All the regional values
        RegionalResultsView(results, selectedMeasurementUnit)
    }
}

/**
 * Displays regional results in a row.
 *
 * @param results The tread depth measurement results.
 * @param selectedUnit The unit for displaying results.
 */
@Composable
fun RegionalResultsView(results: TreadDepthResult, selectedUnit: DevExMeasurementUnit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        results.regions.forEachIndexed { index, region ->
            ResultView(
                name = "Region[$index]",
                result = region,
                measurementUnit = selectedUnit
            )
        }
    }
}

/**
 * Displays a single result value.
 *
 * @param result The result region to be displayed.
 * @param measurementUnit The unit for displaying the result.
 * @param modifier Modifier to be applied to the text.
 */
@Composable
fun ResultView(
    name: String,
    result: TreadResultRegion,
    measurementUnit: DevExMeasurementUnit,
    modifier: Modifier = Modifier
) {
    val value =
        when (measurementUnit) {
            DevExMeasurementUnit.Inch32nds -> result.valueInch32nds.toString()
            DevExMeasurementUnit.Inch -> String.format(
                Locale.getDefault(), "%.2f",
                result.valueInch
            )

            else -> String.format(
                Locale.getDefault(), "%.2f",
                result.valueMm
            )
        }

    Column {
        Text(
            text = name, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 5.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp)
                .width(55.dp)
        )
    }
}

/**
 * Displays the heatmap image.
 *
 * @param heatmapUrl The URL of the heatmap image.
 */
@Composable
fun HeatMapView(heatmapUrl: String) {
    AsyncImage(
        model = heatmapUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(200.dp)
            .shadow(1.dp)
    )
}

/**
 * Composable function that sets up the top app bar for the results screen.
 * It includes a title and a navigation icon to finish the activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultTopAppBar() {
    val activity = LocalActivity.current
    TopAppBar(
        title = { Text(text = "Result Page", color = MaterialTheme.colorScheme.onSurface) },
        navigationIcon = {
            IconButton(onClick = { activity?.finish() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}