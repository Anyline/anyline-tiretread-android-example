package io.anyline.tiretread.devexample.ucr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.ui.components.DevExButton
import io.anyline.tiretread.devexample.ui.components.DevExErrorContent
import io.anyline.tiretread.devexample.ui.components.DevExLoadingView
import io.anyline.tiretread.devexample.ui.components.DevExMeasurementSearch
import io.anyline.tiretread.devexample.ui.components.DevExMeasurementUnit
import io.anyline.tiretread.devexample.ui.components.DevExSectionHeader
import io.anyline.tiretread.devexample.ui.components.DevExUnitSelectionRadioGroup
import io.anyline.tiretread.devexample.ui.components.ucr.DevExRegionalResultsEditView
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity responsible for loading Tread Depth Measurement results, and send user-corrected results.

 * This activity fetches the data based on a provided Measurement UUID and displays the results
 * using Jetpack Compose UI components.
 *
 * @property viewModel The ViewModel that manages the state and data fetching for this activity.
 */
class UcrActivity : ComponentActivity() {

    private val viewModel: UcrViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve the Measurement UUID from the intent
        val measurementUUID = intent.getStringExtra("measurementUUID")

        setContent {
            // Use you own app's theme, and define any UI elements as preferred.
            TTRDeveloperExamplesTheme {
                UcrScreen(viewModel, measurementUUID)
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
fun UcrScreen(
    viewModel: UcrViewModel,
    measurementUUID: String?,
    modifier: Modifier = Modifier
) {
    val isBusy by viewModel.isBusy
    val treadDepthResult by viewModel.treadDepthResult
    val resultsErrorMessage by viewModel.resultFetchErrorMessage
    val feedbackResponse by viewModel.feedbackResponse

    var uuid by rememberSaveable { mutableStateOf(measurementUUID ?: "") }
    var shouldLoadResults by rememberSaveable { mutableStateOf(true) }

    // Effect to fetch results when 'shouldLoadResults' is true
    LaunchedEffect(shouldLoadResults) {
        if (shouldLoadResults) {
            // Fetch results for the given UUID
            viewModel.fetchResults(uuid)
            shouldLoadResults = false
        }
    }

    // Main layout container
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top app bar for the screen
        UcrTopAppBar()

        // Allow searching for a specific Measurement UUID
        // this will probably not be required in your application
        val keyboardController = LocalSoftwareKeyboardController.current
        DevExMeasurementSearch(uuid, onUuidChanged = { uuid = it.trim() }, onDone = {
            shouldLoadResults = true
            keyboardController?.hide()
        })

        // Display content based on the vm state
        when {
            resultsErrorMessage != null -> DevExErrorContent(resultsErrorMessage!!)
            treadDepthResult != null -> {
                ResultFeedbackView(viewModel, uuid, treadDepthResult!!)
                Spacer(modifier = Modifier.height(20.dp))
                CommentFeedbackView(viewModel, uuid)
                if (feedbackResponse != "") {
                    Text(
                        text = "Status: $feedbackResponse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }
            }
        }
    }

    // Display a custom loading indicator on top of the whole screen
    DevExLoadingView(isBusy)
}

@Composable
fun ResultFeedbackView(
    viewModel: UcrViewModel,
    measurementUUID: String,
    results: TreadDepthResult
) {
    var selectedMeasurementUnit by rememberSaveable { mutableStateOf(DevExMeasurementUnit.Mm) }
    val treadResultRegions = remember { results.regions.toMutableList() }
    val regionValues = remember {
        mutableStateListOf<String>().apply {
            addAll(treadResultRegions.map {
                it.getValue(selectedMeasurementUnit)
            })
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DevExSectionHeader("Results:")

        // Radio buttons, allowing the selection of the Measurement Unit to be displayed
        DevExUnitSelectionRadioGroup(selectedMeasurementUnit) { unit ->
            selectedMeasurementUnit = unit
            regionValues.clear()
            regionValues.addAll(treadResultRegions.map { it.getValue(selectedMeasurementUnit) })
        }

        // Show all the regional values as TextFields for editing
        DevExRegionalResultsEditView(
            regionValues = regionValues,
            onValueChanged = { index, newValue ->
                // when the value is updated by the user, we update the list of regional results
                regionValues[index] = newValue
                val updatedRegion =
                    updateRegion(treadResultRegions[index], newValue, selectedMeasurementUnit)
                updatedRegion?.let { treadResultRegions[index] = it }
            })

        // Button to send the tread depth result feedback
        DevExButton(
            text = R.string.send_feedback_result,
            onClick = { viewModel.sendResultFeedback(measurementUUID, treadResultRegions) },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@Composable
fun CommentFeedbackView(
    viewModel: UcrViewModel,
    measurementUUID: String,
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize()
    ) {

        var userComment by remember { mutableStateOf("") }
        TextField(
            value = userComment,
            placeholder = { Text(text = "Comment...") },
            onValueChange = { newValue -> userComment = newValue },
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        // Button to send the comment feedback
        DevExButton(
            text = R.string.send_feedback_comment,
            onClick = { viewModel.sendCommentFeedback(measurementUUID, userComment) },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

// Extension function to get the regional value based on the selected measurement unit
fun TreadResultRegion.getValue(measurementUnit: DevExMeasurementUnit): String {
    return when (measurementUnit) {
        DevExMeasurementUnit.Inch32nds -> valueInch32nds.toString()
        DevExMeasurementUnit.Inch -> String.format(Locale.getDefault(), "%.2f", valueInch)
        else -> String.format(Locale.getDefault(), "%.2f", valueMm)
    }
}

// Function to update the region based on the new value and selected measurement unit
fun updateRegion(
    region: TreadResultRegion,
    newValue: String,
    measurementUnit: DevExMeasurementUnit
): TreadResultRegion? {
    // used to parse a string to double based on the default Locale
    val localizedNumberFormat = NumberFormat.getInstance(Locale.getDefault())

    return when (measurementUnit) {
        DevExMeasurementUnit.Inch32nds -> newValue.toIntOrNull()
            ?.let { TreadResultRegion.initInch32nds(region.isAvailable, it) }

        DevExMeasurementUnit.Inch -> localizedNumberFormat.parse(newValue)?.toDouble()
            ?.let { TreadResultRegion.initInch(region.isAvailable, it) }

        else -> localizedNumberFormat.parse(newValue)?.toDouble()
            ?.let { TreadResultRegion.initMm(region.isAvailable, it) }
    }
}

/**
 * Composable function that sets up the top app bar.
 * It includes a title and a navigation icon to finish the activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UcrTopAppBar() {
    val activity = LocalActivity.current
    TopAppBar(
        title = {
            Text(
                text = "User-Corrected Results",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
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