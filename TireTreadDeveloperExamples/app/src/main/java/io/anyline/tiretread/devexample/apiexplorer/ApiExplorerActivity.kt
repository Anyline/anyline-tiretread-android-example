@file:OptIn(ExperimentalMaterial3Api::class)

package io.anyline.tiretread.devexample.apiexplorer

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.anyline.tiretread.devexample.BuildConfig
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.ui.components.DevExLoadingView
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme
import io.anyline.tiretread.sdk.api.AbortedOutcome
import io.anyline.tiretread.sdk.api.AnylineTireTread
import io.anyline.tiretread.sdk.api.AnylineTireTreadScanner
import io.anyline.tiretread.sdk.api.AnylineTireSidewallScanner
import io.anyline.tiretread.sdk.api.CompletedOutcome
import io.anyline.tiretread.sdk.api.FailedOutcome
import io.anyline.tiretread.sdk.api.TswScanResult
import io.anyline.tiretread.sdk.api.TswSupportStatus
import io.anyline.tiretread.sdk.tsw.ui.configs.TswScannerConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

// Anyline design-system accent colors.
//
// Each accent is used both as a foreground (status text, chip labels, icon
// tints) and as a translucent tint background (`copy(alpha = 0.12f)`). The
// light-mode values mirror the Flutter example. Several are low-luminance —
// notably Warning and AccentCorrelation — and become unreadable on a dark
// surface, so each resolves to a brighter variant in dark mode (CENG-1649).
// Resolving the whole accent (not just its text use) keeps the foreground and
// its tint background in sync across both themes.
private val AccentBrand: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4DB6FF) else Color(0xFF0099FF)
private val AccentSidewall: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF35C4E0) else Color(0xFF0C93B0)
private val AccentCorrelation: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF9B90FF) else Color(0xFF5246E0)
private val Success: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2BD4A8) else Color(0xFF00A37A)
private val Warning: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE8A33C) else Color(0xFFB5740A)

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

    fun launchSidewallScan(correlationId: String?, onResult: (TswScanResult) -> Unit) {
        // Optional TswScannerConfig. Two things you can set:
        //
        //  • config.texts — override the UI strings shown in the scanner
        //    overlay. In a real integration you'd resolve these from your app's
        //    localization pipeline so the wording follows the device locale:
        //
        //        config.texts.textAlignTire = getString(R.string.tsw_align_tire)
        //        config.texts.textHoldSteady = getString(R.string.tsw_hold_steady)
        //
        //    Any field you don't set keeps its English default.
        //
        //  • config.correlationId — an optional v4 UUID multiple Anyline scans can be correlated.
        //    It must be a valid version-4 UUID, otherwise the scan fails fast with
        //    ErrorCode.INVALID_UUID. Here it's driven by the shared
        //    "Include correlationId" switch, which applies to both scans.
        val config = TswScannerConfig().apply {
            this.correlationId = correlationId
        }

        AnylineTireSidewallScanner().scan(
            from = this,
            clientId = BuildConfig.CLOUD_API_CLIENT_ID,
            config = config,
        ) { result ->
            runOnUiThread { onResult(result) }
        }
    }
}

@Composable
fun ExplorerScreen(
    viewModel: ApiExplorerViewModel,
    initialUuid: String
) {
    // Surface (not a bare Column+background) so LocalContentColor resolves to
    // onBackground. Without it, every Text that omits an explicit color falls
    // back to the LocalContentColor default (black) and disappears in dark mode.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
    }

    val isInitBusy by viewModel.isInitBusy
    DevExLoadingView(isInitBusy)
}

@Composable
fun ExplorerContent(
    viewModel: ApiExplorerViewModel,
    initialUuid: String
) {
    val isInitialized by viewModel.isInitialized
    val initError by viewModel.initError

    // Config state
    var appearanceIndex by rememberSaveable { mutableIntStateOf(1) } // Neon
    var scanSpeedIndex by rememberSaveable { mutableIntStateOf(0) } // Fast
    var unitsIndex by rememberSaveable { mutableIntStateOf(0) } // Metric
    var heatmapStyleIndex by rememberSaveable { mutableIntStateOf(0) } // Colored
    var tireWidth by rememberSaveable { mutableStateOf("") }
    var includeCorrelationId by rememberSaveable { mutableStateOf(true) }
    // Generated once per page load; reused for both the tread-depth and sidewall scans.
    val correlationId = rememberSaveable { UUID.randomUUID().toString() }
    var includeTirePosition by rememberSaveable { mutableStateOf(true) }
    var useTireWidthPresets by rememberSaveable { mutableStateOf(false) }

    var uuid by rememberSaveable { mutableStateOf(initialUuid) }
    if (initialUuid.isNotEmpty() && initialUuid != uuid) {
        uuid = initialUuid
    }

    // Sidewall state — lifted here so the completed handler can update tireWidth
    var sidewallStatus by rememberSaveable { mutableStateOf("") }
    var sidewallStatusIsError by rememberSaveable { mutableStateOf(false) }
    var sidewallJson by rememberSaveable { mutableStateOf("") }
    var sidewallJsonExpanded by rememberSaveable { mutableStateOf(false) }
    var sidewallLoading by remember { mutableStateOf(false) }
    // Detected size string + the width forwarded to the Tread config.
    var sidewallSize by rememberSaveable { mutableStateOf("") }
    var widthSentToTread by rememberSaveable { mutableStateOf("") }
    // Backed by the ViewModel so the captured image survives rotation without
    // going into saved-state (too large for the instance-state Bundle).
    var sidewallImage by viewModel.sidewallImage

    // ============ 1 · SET UP ============

    GroupHeader(
        1,
        "Set up",
        trailing = { if (isInitialized) StatusChip("Complete", Success, painterResource(R.drawable.ic_check)) },
    )
    DevExCard {
        val isDeviceSupportBusy by viewModel.isDeviceSupportBusy
        val deviceSupportResult by viewModel.deviceSupportResult
        val deviceSupportIsError by viewModel.deviceSupportIsError
        val isInitBusy by viewModel.isInitBusy
        val activity = LocalActivity.current as? ApiExplorerActivity

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            activity?.let { viewModel.checkDeviceSupport(it) }
        }

        // SDK version row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TTR SDK version",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                AnylineTireTread.sdkVersion,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SetupRow(
            done = deviceSupportResult.isNotEmpty() && !deviceSupportIsError,
            title = "Check device support",
            detail = deviceSupportResult.ifEmpty { "Not checked yet" },
            detailColor = when {
                deviceSupportResult.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                deviceSupportIsError -> MaterialTheme.colorScheme.error
                else -> Success
            },
        ) {
            SoftButton(if (deviceSupportResult.isEmpty()) "Check" else "Re-check", busy = isDeviceSupportBusy) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SetupRow(
            done = isInitialized,
            title = "Initialize SDK",
            detail = when {
                isInitialized -> "Initialized · ready to scan"
                initError.isNotEmpty() -> initError
                else -> "Not initialized"
            },
            detailColor = when {
                isInitialized -> Success
                initError.isNotEmpty() -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            SoftButton("Initialize", busy = isInitBusy) {
                activity?.let { viewModel.initializeSDK(BuildConfig.LICENSE_KEY, it) }
            }
        }
    }

    // ============ CORRELATION ID (optional, shared by both scanners) ============

    DevExCard(
        title = "Correlation ID",
        subtitle = "Links one sidewall + one tread scan as a pair. Applies to both scanners below.",
        accent = AccentCorrelation,
        leading = { IconTile(painterResource(R.drawable.ic_correlation_link), AccentCorrelation, size = 34.dp) },
        titleTrailing = { MutedChip("Optional") },
        status = { Switch(checked = includeCorrelationId, onCheckedChange = { includeCorrelationId = it }) },
    ) {
        if (includeCorrelationId) MonoInset(correlationId)
    }

    // ============ 2 · SCAN ============

    GroupHeader(2, "Scan", hint = "Two independent scanners")

    // ---- Scanner A — Tire Sidewall ----
    val activitySidewall = LocalActivity.current as? ApiExplorerActivity
    var sidewallSupport by remember { mutableStateOf<TswSupportStatus?>(null) }
    LaunchedEffect(Unit) {
        sidewallSupport = AnylineTireSidewallScanner.isSupported()
    }
    val sidewallSupported = sidewallSupport is TswSupportStatus.Supported

    DevExCard(
        title = "Tire Sidewall",
        subtitle = "Reads tire size markings off the sidewall",
        accent = AccentBrand,
        leading = { IconTile(painterResource(R.drawable.ic_tire_sidewall), AccentBrand) },
        status = {
            when (sidewallSupport) {
                null -> MutedChip("Checking…")
                is TswSupportStatus.Supported -> StatusChip("Supported", Success)
                is TswSupportStatus.Unavailable -> StatusChip("Not supported", MaterialTheme.colorScheme.error)
            }
        },
    ) {
        if (includeCorrelationId) AttachedChip()

        (sidewallSupport as? TswSupportStatus.Unavailable)?.let { support ->
            Text(
                "Sidewall scanner unavailable: ${support.error.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            if (support.userResolvable && activitySidewall != null) {
                ActionButton("Fix Play Services", MaterialTheme.colorScheme.error) {
                    AnylineTireSidewallScanner.resolvePlayServices(activitySidewall)
                }
            }
        }

        ActionButton(
            "Sidewall Scan",
            AccentBrand,
            enabled = sidewallSupported && !sidewallLoading,
            leadingIcon = painterResource(R.drawable.ic_center_focus_strong),
        ) {
            sidewallStatus = "Scanning…"
            sidewallStatusIsError = false
            sidewallJson = ""
            sidewallJsonExpanded = false
            sidewallSize = ""
            widthSentToTread = ""
            sidewallImage = null
            sidewallLoading = true
            activitySidewall?.launchSidewallScan(
                correlationId = if (includeCorrelationId) correlationId else null,
            ) { result ->
                sidewallLoading = false
                when (result) {
                    is TswScanResult.Completed -> {
                        sidewallStatusIsError = false
                        sidewallImage = result.imageBytes
                        val parsed = try { JSONObject(result.resultJson) } catch (_: Exception) { null }
                        sidewallJson = parsed?.toString(2) ?: result.resultJson
                        sidewallStatus = "Completed"
                        val sizeString = parsed?.optString("size")?.takeIf { it.isNotEmpty() }
                        sidewallSize = sizeString ?: ""
                        sizeString
                            ?.let { extractTireWidthFromTireSizeString(it) }
                            ?.let {
                                tireWidth = it.toString()
                                widthSentToTread = it.toString()
                            }
                    }
                    is TswScanResult.Aborted -> {
                        sidewallStatus = "Sidewall scan aborted"
                        sidewallStatusIsError = false
                    }
                    is TswScanResult.Failed -> {
                        sidewallStatus = "Failed (${result.error.code}): ${result.error.message}"
                        sidewallStatusIsError = true
                    }
                }
            }
        }

        if (sidewallLoading || sidewallStatus.isNotEmpty()) {
            val isAborted = sidewallStatus.contains("aborted", ignoreCase = true)
            val color = when {
                sidewallStatusIsError -> MaterialTheme.colorScheme.error
                isAborted -> Warning
                sidewallLoading -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> Success
            }
            val icon = when {
                sidewallLoading -> null
                sidewallStatusIsError || isAborted -> painterResource(R.drawable.ic_error)
                else -> painterResource(R.drawable.ic_check)
            }
            StatusLine(sidewallStatus.ifEmpty { "Scanning…" }, color = color, busy = sidewallLoading, icon = icon)
        }

        // Captured image + detected size + width handoff
        val bytes = sidewallImage
        if (bytes != null || sidewallSize.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (bytes != null) {
                    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    if (bitmap != null) {
                        // The sidewall scanner returns a 3:4 (portrait) capture —
                        // size the thumbnail to match so it fills without letterboxing.
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured tire sidewall",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(90.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF101114)),
                        )
                    }
                }
                if (sidewallSize.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "DETECTED SIZE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            sidewallSize,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (widthSentToTread.isNotEmpty()) {
                            HandoffChip("Width $widthSentToTread mm sent to Tread")
                        }
                    }
                }
            }
        }

        if (sidewallJson.isNotEmpty()) {
            Disclosure("Result JSON", sidewallJsonExpanded, { sidewallJsonExpanded = !sidewallJsonExpanded }) {
                Text(
                    sidewallJson,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(11.dp),
                )
            }
        }
    }

    // ---- Scanner B — Tire Tread ----
    DevExCard(
        title = "Tire Tread",
        subtitle = "Measures tread depth across the tire",
        accent = AccentBrand,
        leading = { IconTile(painterResource(R.drawable.ic_tire_tread), AccentBrand) },
        status = { if (isInitialized) StatusChip("Ready", Success) else MutedChip("Needs setup") },
    ) {
        SubHead("Scan configuration")
        SegmentedRow("Appearance", listOf("Classic", "Neon"), appearanceIndex) { appearanceIndex = it }
        SegmentedRow("Scan Speed", listOf("Fast", "Slow"), scanSpeedIndex) { scanSpeedIndex = it }
        SegmentedRow("Units", listOf("Metric", "Imperial"), unitsIndex) { unitsIndex = it }
        SegmentedRow("Heatmap", listOf("Colored", "Grayscale"), heatmapStyleIndex) { heatmapStyleIndex = it }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tire width (mm)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tireWidth.isNotEmpty() && tireWidth == widthSentToTread) {
                FromTag("from sidewall")
                Spacer(Modifier.width(8.dp))
            }
            OutlinedTextField(
                value = tireWidth,
                onValueChange = {
                    tireWidth = it.filter { c -> c.isDigit() }
                    widthSentToTread = "" // manual edit clears the "from sidewall" tag
                },
                placeholder = { Text("not set") },
                modifier = Modifier.width(120.dp),
                singleLine = true,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SwitchRow("Include tirePosition", includeTirePosition, sub = "Added to additionalContext") { includeTirePosition = it }
        SwitchRow("Tire width presets (Android)", useTireWidthPresets, sub = "Offer 205 / 215 / 225 / 235") { useTireWidthPresets = it }

        val activity = LocalActivity.current as? ApiExplorerActivity
        ActionButton("Tread Scan", AccentBrand, enabled = isInitialized, leadingIcon = painterResource(R.drawable.ic_crop_free)) {
            viewModel.clearScanOutcome()
            val json = buildConfigJson(
                appearanceIndex = appearanceIndex,
                scanSpeedIndex = scanSpeedIndex,
                unitsIndex = unitsIndex,
                heatmapStyleIndex = heatmapStyleIndex,
                tireWidth = tireWidth,
                includeCorrelationId = includeCorrelationId,
                correlationId = correlationId,
                includeTirePosition = includeTirePosition,
                useTireWidthPresets = useTireWidthPresets
            )
            activity?.launchScan(json)
        }

        val scanStatus by viewModel.scanStatus
        val scanStatusIsError by viewModel.scanStatusIsError
        if (scanStatus.isNotEmpty()) {
            StatusLine(
                scanStatus,
                color = when {
                    scanStatusIsError -> MaterialTheme.colorScheme.error
                    scanStatus.contains("aborted", ignoreCase = true) -> Warning
                    else -> Success
                },
            )
        }

        OutlinedTextField(
            value = uuid,
            onValueChange = { uuid = it.trim() },
            label = { Text("Measurement UUID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    // ============ 3 · RESULTS (from the Tread scan above) ============

    GroupHeader(3, "Results", hint = "From the Tread scan above")
    DevExCard {
        val isResultsBusy by viewModel.isResultsBusy
        val resultsStatus by viewModel.resultsStatus
        val resultsIsError by viewModel.resultsIsError
        val result by viewModel.treadDepthResult

        SpinnerButton("Get Results", isResultsBusy, outlined = true) {
            if (uuid.isNotEmpty()) viewModel.fetchResults(uuid)
        }

        if (resultsStatus.isNotEmpty()) {
            StatusLine(resultsStatus, color = if (resultsIsError) MaterialTheme.colorScheme.error else Success)
        }

        result?.let { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Global", formatMm(r.global.valueMm), modifier = Modifier.weight(1f))
                MetricTile("Minimum", formatMm(r.minimumValue.valueMm), highlight = true, modifier = Modifier.weight(1f))
            }
            if (r.regions.isNotEmpty()) {
                SubHead("Per region")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    r.regions.forEachIndexed { i, region ->
                        RegionTile("R$i", formatMm(region.valueMm), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// === Composable toolkit ===

/** Rounded, hairline-bordered surface that hosts one logical section. */
@Composable
fun DevExCard(
    title: String? = null,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    leading: @Composable (() -> Unit)? = null,
    titleTrailing: @Composable (() -> Unit)? = null,
    status: @Composable (() -> Unit)? = null,
    stripe: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Surface (not Column+background) so card content inherits onSurface as its
    // LocalContentColor — color-less Text (e.g. the card title) then adapts to
    // dark mode instead of defaulting to black.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            if (stripe != null) Box(Modifier.fillMaxWidth().height(3.dp).background(stripe))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null || status != null || leading != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(12.dp))
                    }
                    if (title != null) {
                        Column(Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                titleTrailing?.invoke()
                            }
                            if (subtitle != null) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    status?.invoke()
                }
            }
            content()
            }
        }
    }
}

/** Numbered group label (1 / 2 / 3) that makes order-of-operations explicit. */
@Composable
fun GroupHeader(number: Int, title: String, hint: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.onBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.weight(1f))
        when {
            trailing != null -> trailing()
            hint != null -> Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Soft, tinted status pill (success / etc.), with an optional leading icon. */
@Composable
fun StatusChip(text: String, color: Color, leadingIcon: Painter? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** Subtle, outlined pill for non-status labels ("Optional", "Needs setup"). */
@Composable
fun MutedChip(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Compact, brand-tinted secondary button used inside setup rows. */
@Composable
fun SoftButton(label: String, busy: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val active = enabled && !busy
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) AccentBrand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AccentBrand)
        } else {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (active) AccentBrand else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Prerequisite row: status circle + title/detail + a trailing action. */
@Composable
fun SetupRow(done: Boolean, title: String, detail: String, detailColor: Color, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) Color(0xFF00BB8E) else MaterialTheme.colorScheme.surfaceVariant)
                .then(if (done) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = detailColor)
        }
        trailing()
    }
}

/** Left-aligned pill marking a scanner as carrying the shared correlationId. */
@Composable
fun AttachedChip() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentCorrelation.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Icon(painterResource(R.drawable.ic_correlation_link), contentDescription = null, tint = AccentCorrelation, modifier = Modifier.size(13.dp))
        Text("correlationId attached", color = AccentCorrelation, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** Rounded, accent-tinted container for a scanner glyph. */
@Composable
fun IconTile(painter: Painter, accent: Color, size: Dp = 38.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painter, contentDescription = null, tint = accent, modifier = Modifier.size(size * 0.62f))
    }
}

/** Monospace inset row (UUID etc.). */
@Composable
fun MonoInset(value: String, tag: String = "UUID", tagColor: Color = AccentCorrelation) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(tag, color = tagColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small uppercase section label inside a card. */
@Composable
fun SubHead(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
    )
}

/** Tap-to-expand row: a chevron + title that reveals [content] when expanded. */
@Composable
fun Disclosure(title: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp).rotate(if (expanded) 90f else 0f),
            )
            Spacer(Modifier.width(6.dp))
            SubHead(title)
        }
        if (expanded) content()
    }
}

/** Teal tag flagging a value handed over from the sidewall scan. */
@Composable
fun FromTag(text: String) {
    Text(
        text,
        color = AccentSidewall,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(AccentSidewall.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 5.dp),
    )
}

/** Brand chip announcing the width handed from Sidewall to Tread. */
@Composable
fun HandoffChip(text: String) {
    Text(
        text,
        color = AccentBrand,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(AccentBrand.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
    )
}

/** Status text with an optional inline spinner or leading icon. */
@Composable
fun StatusLine(text: String, color: Color, busy: Boolean = false, icon: Painter? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/** Big result tile (Global / Minimum); highlighted tiles use the brand accent. */
@Composable
fun MetricTile(label: String, value: String, unit: String = "mm", highlight: Boolean = false, modifier: Modifier = Modifier) {
    val bg = if (highlight) AccentBrand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val valueColor = if (highlight) AccentBrand else MaterialTheme.colorScheme.onSurface
    val labelColor = if (highlight) AccentBrand else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(bg).padding(13.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = labelColor, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(Modifier.width(3.dp))
            Text(unit, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = labelColor, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/** Compact per-region result tile. */
@Composable
fun RegionTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SegmentedRow(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun SwitchRow(
    label: String,
    checked: Boolean,
    sub: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ActionButton(text: String, color: Color, enabled: Boolean = true, contentColor: Color = Color.White, leadingIcon: Painter? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpinnerButton(text: String, isBusy: Boolean, color: Color = AccentBrand, outlined: Boolean = false, onClick: () -> Unit) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = !isBusy,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, color),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = color, strokeWidth = 2.dp)
            } else {
                Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White,
                disabledContainerColor = color.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_anyline_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("TireTread API Explorer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Try every SDK option, end to end", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

private val DEFAULT_TIRE_WIDTH_PRESETS = listOf(205, 215, 225, 235)

private fun formatMm(value: Number): String = String.format(Locale.getDefault(), "%.2f", value)

private fun buildConfigJson(
    appearanceIndex: Int,
    scanSpeedIndex: Int,
    unitsIndex: Int,
    heatmapStyleIndex: Int,
    tireWidth: String,
    includeCorrelationId: Boolean,
    correlationId: String,
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
            additionalContext.put("correlationId", correlationId)
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

private fun extractTireWidthFromTireSizeString(tireSizeString: String): Int? {
    val regex = Regex("""[A-Za-z]*\d{3}""")
    val match = regex.find(tireSizeString)
    val tireWidth = match?.value?.filter { it.isDigit() }?.take(3)?.toIntOrNull()
    return if (tireWidth in 100..500) tireWidth else null
}
