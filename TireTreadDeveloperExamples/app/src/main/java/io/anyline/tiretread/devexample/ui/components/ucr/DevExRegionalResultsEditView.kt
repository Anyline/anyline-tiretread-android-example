package io.anyline.tiretread.devexample.ui.components.ucr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DevExRegionalResultsEditView(
    regionValues: List<String>,
    onValueChanged: (Int, String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        regionValues.forEachIndexed { index, value ->
            DevExResultEditView(
                name = "Region[$index]",
                textFieldValue = value,
                onValueChanged = { newValue -> onValueChanged(index, newValue) },
            )
        }
    }
}
