package io.anyline.tiretread.devexample.scan_process

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.ui.components.DevExButton
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activity for selecting JSON configuration and layout type for tire tread scanning.
 * This demonstrates how to use the SDK with custom JSON configurations.
 */
class JsonConfigSelectionActivity(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TTRDeveloperExamplesTheme {
                ScaffoldAndTopBar(content = {
                    JsonConfigSelectionContent(
                        modifier = Modifier.padding(it)
                    )
                })
            }
        }
    }

    @Composable
    private fun JsonConfigSelectionContent(
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        var jsonConfigs by remember { mutableStateOf<List<String>>(emptyList()) }
        var selectedConfig by remember { mutableStateOf<String?>(null) }
        var selectedLayout by remember { mutableStateOf(LayoutType.COMPOSE) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Load and display JSON config files from assets
        LaunchedEffect(Unit) {
            isLoading = true
            loadJsonConfigs(context)
                .onSuccess { configs ->
                    jsonConfigs = configs
                    if (selectedConfig == null) {
                        selectedConfig = configs.firstOrNull()
                    }
                    errorMessage = null
                }
                .onFailure { throwable ->
                    jsonConfigs = emptyList()
                    val message = throwable.message ?: "Unknown error"
                    errorMessage = "Failed to load JSON configurations: $message"
                }
            isLoading = false
        }

        val loadState = when {
            isLoading -> JsonConfigLoadState.Loading
            errorMessage != null -> JsonConfigLoadState.Error(errorMessage!!)
            jsonConfigs.isEmpty() -> JsonConfigLoadState.Empty
            else -> JsonConfigLoadState.Ready(jsonConfigs)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = getString(R.string.select_json_config),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            JsonConfigStateContent(
                state = loadState,
                selectedConfig = selectedConfig,
                onConfigSelected = { selectedConfig = it }
            )

            Text(
                text = getString(R.string.select_layout_type),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Layout Type Selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                LayoutType.entries.forEach { layoutType ->
                    LayoutOption(
                        text = when (layoutType) {
                            LayoutType.COMPOSE -> "Compose\nLayout"
                            LayoutType.XML -> "XML\nLayout"
                        },
                        selected = selectedLayout == layoutType,
                        onSelected = { selectedLayout = layoutType }
                    )
                }
            }

            // Start Scan Button - only show when we have valid configurations
            if (loadState is JsonConfigLoadState.Ready) {
                DevExButton(
                    text = R.string.start_scan,
                    isEnabled = selectedConfig != null
                ) {
                    selectedConfig?.let { config ->
                        val intent = when (selectedLayout) {
                            LayoutType.COMPOSE -> Intent(context, ComposeScanActivity::class.java)
                            LayoutType.XML -> Intent(context, XmlScanActivity::class.java)
                        }
                        intent.putExtra("jsonConfigPath", "tire-tread-configs/$config")
                        context.startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    private fun JsonConfigStateContent(
        state: JsonConfigLoadState,
        selectedConfig: String?,
        onConfigSelected: (String) -> Unit
    ) {
        when (state) {
            JsonConfigLoadState.Loading -> JsonConfigMessage(
                text = "Loading JSON configurations...",
                color = MaterialTheme.colorScheme.onBackground
            )

            is JsonConfigLoadState.Error -> JsonConfigMessage(
                text = state.message,
                color = MaterialTheme.colorScheme.error
            )

            JsonConfigLoadState.Empty -> JsonConfigMessage(
                text = "No JSON configurations found in assets/tire-tread-configs/",
                color = MaterialTheme.colorScheme.onBackground
            )

            is JsonConfigLoadState.Ready -> JsonConfigDropdown(
                configs = state.configs,
                selectedConfig = selectedConfig,
                onConfigSelected = onConfigSelected
            )
        }
    }

    @Composable
    private fun JsonConfigMessage(text: String, color: Color) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun JsonConfigDropdown(
        configs: List<String>,
        selectedConfig: String?,
        onConfigSelected: (String) -> Unit
    ) {
        var isDropdownExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = selectedConfig.orEmpty(),
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = isDropdownExpanded
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                configs.forEach { config ->
                    DropdownMenuItem(
                        text = { Text(config) },
                        onClick = {
                            onConfigSelected(config)
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }


    private suspend fun loadJsonConfigs(context: Context): Result<List<String>> =
        withContext(ioDispatcher) {
            runCatching {
                val configFolder = "tire-tread-configs"
                context.assets
                    .list(configFolder)
                    ?.filter { it.endsWith(".json") }
                    ?.sorted()
                    ?: emptyList()
            }
        }

    private sealed interface JsonConfigLoadState {
        data object Loading : JsonConfigLoadState
        data object Empty : JsonConfigLoadState
        data class Error(val message: String) : JsonConfigLoadState
        data class Ready(val configs: List<String>) : JsonConfigLoadState
    }

    @Composable
    private fun LayoutOption(
        text: String,
        selected: Boolean,
        onSelected: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .selectable(
                    selected = selected,
                    onClick = onSelected,
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelected
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ScaffoldAndTopBar(
        content: @Composable (PaddingValues) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = getString(R.string.select_json_config),
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

    private enum class LayoutType {
        COMPOSE, XML
    }
}
