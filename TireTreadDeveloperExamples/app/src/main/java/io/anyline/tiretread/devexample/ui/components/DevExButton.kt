package io.anyline.tiretread.devexample.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme

/**
 * Sets up a button with the specified text and click action.
 *
 * @param text The resource ID of the text to be displayed on the button.
 * @param modifier Modifier to be applied to the button.
 * @param isEnabled Boolean indicating if the button is enabled.
 * @param onClick Callback to handle the button click.
 */
@Composable
fun DevExButton(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        enabled = isEnabled,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = text),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@PreviewLightDark
@Composable
fun DevExButtonPreview() {
    TTRDeveloperExamplesTheme {
        DevExButton(text = R.string.default_config_compose) { }
    }
}
