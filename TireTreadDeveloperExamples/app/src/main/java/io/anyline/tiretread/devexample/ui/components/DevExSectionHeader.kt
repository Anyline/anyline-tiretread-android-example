package io.anyline.tiretread.devexample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme

/**
 * Composable function that sets up a section header with the specified title.
 *
 * @param title The title to be displayed in the section header.
 */
@Composable
fun DevExSectionHeader(title: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@PreviewLightDark
@Composable
fun SectionHeaderPreview(modifier: Modifier = Modifier) {
    TTRDeveloperExamplesTheme {
        DevExSectionHeader(title = "Header title...", modifier = modifier)
    }
}