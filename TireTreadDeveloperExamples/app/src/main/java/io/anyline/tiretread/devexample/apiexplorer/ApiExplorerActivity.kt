@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package io.anyline.tiretread.devexample.apiexplorer

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.anyline.tiretread.devexample.BuildConfig
import io.anyline.tiretread.devexample.ui.components.DevExLoadingView
import io.anyline.tiretread.devexample.ui.components.DevExSectionHeader
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme
import io.anyline.tiretread.sdk.api.AbortedOutcome
import io.anyline.tiretread.sdk.api.AnylineTireTread
import io.anyline.tiretread.sdk.api.AnylineTireTreadScanner
import io.anyline.tiretread.sdk.api.CompletedOutcome
import io.anyline.tiretread.sdk.api.FailedOutcome
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ApiExplorerActivity : ComponentActivity() {

    private val viewModel: ApiExplorerViewModel by viewModels()
    private val scannedUuid = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // SDK init is triggered by the Initialize button

        setContent {
            TTRDeveloperExamplesTheme {
                ExplorerScreen(viewModel, scannedUuid.value)
            }
        }
    }

    fun launchScan(configJson: String?) {
        AnylineTireTreadScanner().scan(from = this, configJson = configJson) { outcome ->
            when (outcome) {
                is CompletedOutcome -> {
                    Log.i("TTRDevExample", "Scan completed: ${outcome.measurementUUID}")
                    runOnUiThread {
                        scannedUuid.value = outcome.measurementUUID
                        viewModel.setScanCompleted(outcome.measurementUUID)
                    }
                }
                is AbortedOutcome -> {
                    Log.i("TTRDevExample", "Scan aborted")
                    runOnUiThread {
                        viewModel.setScanAborted()
                    }
                }
                is FailedOutcome -> {
                    Log.e("TTRDevExample", "Scan failed: ${outcome.error.message}")
                    runOnUiThread {
                        viewModel.setScanFailed(outcome.error.message)
                        Toast.makeText(this, "Scan failed: ${outcome.error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

@Composable
fun ExplorerScreen(
    viewModel: ApiExplorerViewModel,
    initialUuid: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            ExplorerContent(viewModel, initialUuid)
        }
    }

    val isInitBusy by viewModel.isInitBusy
    DevExLoadingView(isInitBusy)
}

@Composable
fun ExplorerContent(
    viewModel: ApiExplorerViewModel,
    initialUuid: String
) {
    val pad = 20.dp

    val isInitialized by viewModel.isInitialized
    val initError by viewModel.initError

    // Config state
    var appearanceIndex by rememberSaveable { mutableIntStateOf(1) } // Neon
    var scanSpeedIndex by rememberSaveable { mutableIntStateOf(0) } // Fast
    var unitsIndex by rememberSaveable { mutableIntStateOf(0) } // Metric
    var heatmapStyleIndex by rememberSaveable { mutableIntStateOf(0) } // Colored
    var tireWidth by rememberSaveable { mutableStateOf("") }
    var includeCorrelationId by rememberSaveable { mutableStateOf(true) }
    var includeTirePosition by rememberSaveable { mutableStateOf(true) }
    var useTireWidthPresets by rememberSaveable { mutableStateOf(false) }

    var uuid by rememberSaveable { mutableStateOf(initialUuid) }
    if (initialUuid.isNotEmpty() && initialUuid != uuid) {
        uuid = initialUuid
    }

    // === SECTION: Initialize ===

    DevExSectionHeader("Initialize")

    Column(
        modifier = Modifier.padding(horizontal = pad),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SDK Version: ${AnylineTireTread.sdkVersion}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        val isInitBusy by viewModel.isInitBusy
        val activity = LocalActivity.current as? ApiExplorerActivity

        SpinnerButton("Initialize", isInitBusy, color = Color(0xFF007AFF)) {
            activity?.let { viewModel.initializeSDK(BuildConfig.LICENSE_KEY, it) }
        }

        if (isInitialized) {
            Text(
                "SDK initialized",
                color = Color(0xFF34C759),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (initError.isNotEmpty()) {
            Text(
                initError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // === SECTION: Scan Config ===

    DevExSectionHeader("Scan Config")

    Column(
        modifier = Modifier.padding(horizontal = pad),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SegmentedRow("Appearance", listOf("Classic", "Neon"), appearanceIndex) { appearanceIndex = it }
        SegmentedRow("Scan Speed", listOf("Fast", "Slow"), scanSpeedIndex) { scanSpeedIndex = it }
        SegmentedRow("Units", listOf("Metric", "Imperial"), unitsIndex) { unitsIndex = it }
        SegmentedRow("Heatmap", listOf("Colored", "Grayscale"), heatmapStyleIndex) { heatmapStyleIndex = it }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tire Width (mm)", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
            OutlinedTextField(
                value = tireWidth,
                onValueChange = { tireWidth = it.filter { c -> c.isDigit() } },
                placeholder = { Text("not set") },
                modifier = Modifier.width(120.dp),
                singleLine = true
            )
        }

        SwitchRow("Include correlationId", includeCorrelationId) { includeCorrelationId = it }
        SwitchRow("Include tirePosition", includeTirePosition) { includeTirePosition = it }
        Text(
            text = buildAdditionalContextSummary(includeCorrelationId, includeTirePosition),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        SwitchRow("Tire width presets (Android)", useTireWidthPresets) { useTireWidthPresets = it }

        Spacer(modifier = Modifier.height(8.dp))

        val activity = LocalActivity.current as? ApiExplorerActivity
        ActionButton("Scan", Color(0xFF0099FF), enabled = isInitialized) {
            viewModel.clearScanOutcome()
            val json = buildConfigJson(
                appearanceIndex = appearanceIndex,
                scanSpeedIndex = scanSpeedIndex,
                unitsIndex = unitsIndex,
                heatmapStyleIndex = heatmapStyleIndex,
                tireWidth = tireWidth,
                includeCorrelationId = includeCorrelationId,
                includeTirePosition = includeTirePosition,
                useTireWidthPresets = useTireWidthPresets
            )
            activity?.launchScan(json)
        }

        OutlinedTextField(
            value = uuid,
            onValueChange = { uuid = it.trim() },
            label = { Text("Measurement UUID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val scanStatus by viewModel.scanStatus
        val scanStatusIsError by viewModel.scanStatusIsError
        if (scanStatus.isNotEmpty()) {
            Text(
                scanStatus,
                color = when {
                    scanStatusIsError -> MaterialTheme.colorScheme.error
                    scanStatus.contains("aborted", ignoreCase = true) -> Color(0xFFFF9800)
                    else -> Color(0xFF34C759)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // === SECTION: Results ===

    DevExSectionHeader("Results")

    Column(modifier = Modifier.padding(horizontal = pad), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val isResultsBusy by viewModel.isResultsBusy
        val resultsStatus by viewModel.resultsStatus
        val resultsIsError by viewModel.resultsIsError
        val result by viewModel.treadDepthResult

        SpinnerButton("Get Results", isResultsBusy) {
            if (uuid.isNotEmpty()) viewModel.fetchResults(uuid)
        }

        if (resultsStatus.isNotEmpty()) {
            Text(resultsStatus, color = if (resultsIsError) MaterialTheme.colorScheme.error else Color(0xFF34C759), style = MaterialTheme.typography.bodySmall)
        }

        result?.let { r ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                DepthValue("Global", String.format(Locale.getDefault(), "%.2f", r.global.valueMm))
                DepthValue("Minimum", String.format(Locale.getDefault(), "%.2f", r.minimumValue.valueMm))
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                r.regions.forEachIndexed { i, region ->
                    DepthValue("R[$i]", String.format(Locale.getDefault(), "%.2f", region.valueMm))
                }
            }
        }
    }
}

// === Composable Helpers ===

@Composable
fun SegmentedRow(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { i, text ->
                FilterChip(
                    selected = selectedIndex == i,
                    onClick = { onSelect(i) },
                    label = { Text(text, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, labelColor: Color = MaterialTheme.colorScheme.onBackground, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ActionButton(text: String, color: Color, enabled: Boolean = true, contentColor: Color = Color.White, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpinnerButton(text: String, isBusy: Boolean, color: Color = Color(0xFF007AFF), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DepthValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = "$value\nmm",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
                .width(65.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = { Text("TireTread Developer Examples", color = MaterialTheme.colorScheme.onSurface) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

private const val DEFAULT_CORRELATION_ID = "123e4567-e89b-12d3-a456-426614174000"
private val DEFAULT_TIRE_WIDTH_PRESETS = listOf(205, 215, 225, 235)

private fun buildConfigJson(
    appearanceIndex: Int,
    scanSpeedIndex: Int,
    unitsIndex: Int,
    heatmapStyleIndex: Int,
    tireWidth: String,
    includeCorrelationId: Boolean,
    includeTirePosition: Boolean,
    useTireWidthPresets: Boolean
): String {
    val config = JSONObject()

    val scanConfig = JSONObject()
    scanConfig.put("heatmapStyle", if (heatmapStyleIndex == 0) "Colored" else "Grayscale")
    tireWidth.toIntOrNull()?.takeIf { it > 0 }?.let { scanConfig.put("tireWidth", it) }
    config.put("scanConfig", scanConfig)

    if (includeCorrelationId || includeTirePosition) {
        val additionalContext = JSONObject()
        if (includeCorrelationId) {
            additionalContext.put("correlationId", DEFAULT_CORRELATION_ID)
        }
        if (includeTirePosition) {
            additionalContext.put(
                "tirePosition",
                JSONObject()
                    .put("axle", 1)
                    .put("positionOnAxle", 1)
                    .put("side", "Left")
            )
        }
        config.put("additionalContext", additionalContext)
    }

    val uiConfig = JSONObject()
    uiConfig.put("measurementSystem", if (unitsIndex == 0) "Metric" else "Imperial")
    uiConfig.put("appearance", arrayOf("Classic", "Neon")[appearanceIndex])
    uiConfig.put("scanSpeed", if (scanSpeedIndex == 0) "Fast" else "Slow")

    if (useTireWidthPresets) {
        val tireWidthInputConfig = JSONObject()
        tireWidthInputConfig.put("tireWidthOptions", JSONArray(DEFAULT_TIRE_WIDTH_PRESETS))
        uiConfig.put("tireWidthInputConfig", tireWidthInputConfig)
    }

    config.put("uiConfig", uiConfig)
    return config.toString(2)
}

private fun buildAdditionalContextSummary(
    includeCorrelationId: Boolean,
    includeTirePosition: Boolean
): String {
    val parts = mutableListOf<String>()
    if (includeCorrelationId) parts += "correlationId"
    if (includeTirePosition) parts += "tirePosition"
    return if (parts.isEmpty()) "No additional context" else "Additional context: ${parts.joinToString(" + ")}"
}
