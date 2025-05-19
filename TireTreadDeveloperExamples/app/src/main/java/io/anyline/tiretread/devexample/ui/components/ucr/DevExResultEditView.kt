package io.anyline.tiretread.devexample.ui.components.ucr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DevExResultEditView(
    name: String,
    textFieldValue: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        Text(
            text = name, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 5.dp)
        )
        TextField(
            value = textFieldValue,
            onValueChange = onValueChanged,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .width(70.dp)
        )
    }
}