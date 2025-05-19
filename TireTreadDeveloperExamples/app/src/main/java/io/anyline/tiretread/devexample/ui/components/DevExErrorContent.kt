package io.anyline.tiretread.devexample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme

/**
 * Displays an error message.
 *
 * @param message The error message to be displayed.
 * @param modifier Modifier to be applied to the box.
 */
@Composable
fun DevExErrorContent(message: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Error:\n$message",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(10.dp)
        )
    }
}

@Composable
@PreviewLightDark
fun ErrorContentPreview(modifier: Modifier = Modifier) {
    TTRDeveloperExamplesTheme {
        DevExErrorContent(message = "123: There was an error doing something", modifier = modifier)
    }
}