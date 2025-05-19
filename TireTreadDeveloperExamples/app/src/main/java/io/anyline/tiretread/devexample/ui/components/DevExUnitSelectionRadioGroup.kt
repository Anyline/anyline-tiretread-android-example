package io.anyline.tiretread.devexample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme

/**
 * Example enum class, used to manage the unit selection when displaying results
 */
enum class DevExMeasurementUnit { Mm, Inch, Inch32nds }

/**
 * Sets up radio buttons for unit selection.
 * It allows the user to select the unit for displaying results.
 *
 * @param selectedMeasurementUnit The currently selected unit.
 * @param onUnitSelected Callback to handle unit selection changes.
 */
@Composable
fun DevExUnitSelectionRadioGroup(
    selectedMeasurementUnit: DevExMeasurementUnit,
    modifier: Modifier = Modifier,
    onUnitSelected: (DevExMeasurementUnit) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(color = MaterialTheme.colorScheme.background)
    ) {
        Text(
            "Select Unit:",
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedMeasurementUnit == DevExMeasurementUnit.Mm,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { onUnitSelected(DevExMeasurementUnit.Mm) }
            )

            Text(
                "mm",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )

            RadioButton(
                selected = selectedMeasurementUnit == DevExMeasurementUnit.Inch,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { onUnitSelected(DevExMeasurementUnit.Inch) }
            )
            Text(
                "inch",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )

            RadioButton(
                selected = selectedMeasurementUnit == DevExMeasurementUnit.Inch32nds,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { onUnitSelected(DevExMeasurementUnit.Inch32nds) }
            )
            Text(
                "inch32nds",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun UnitSelectionRadioGroupPreview(modifier: Modifier = Modifier) {
    TTRDeveloperExamplesTheme {
        DevExUnitSelectionRadioGroup(
            selectedMeasurementUnit = DevExMeasurementUnit.Inch,
            modifier = modifier
        ) {

        }
    }
}