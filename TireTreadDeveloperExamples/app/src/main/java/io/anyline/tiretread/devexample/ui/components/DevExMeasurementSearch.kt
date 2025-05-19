package io.anyline.tiretread.devexample.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme

/**
 * Sets up an input field for the Measurement UUID and a search button.
 * It allows the user to enter a UUID and triggers an action when the done button is pressed.
 *
 * @param measurementUuid The current UUID value.
 * @param onUuidChanged Callback to handle changes to the UUID input.
 * @param onDone Callback to handle the done action.
 */
@Composable
fun DevExMeasurementSearch(
    measurementUuid: String,
    onUuidChanged: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        OutlinedTextField(
            value = measurementUuid,
            onValueChange = onUuidChanged,
            label = { Text("Measurement UUID") },
            colors = OutlinedTextFieldDefaults.colors(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        DevExButton(
            text = R.string.load_results,
            onClick = onDone,
            modifier = modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeasurementSearchPreview(modifier: Modifier = Modifier) {
    TTRDeveloperExamplesTheme {
        DevExMeasurementSearch(
            measurementUuid = "ABC-123-XYZ",
            onUuidChanged = {},
            onDone = {},
            modifier = modifier
        )
    }
}
