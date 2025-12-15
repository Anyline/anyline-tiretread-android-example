package io.anyline.tiretread.devexample.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF007AFF), // iOS system blue
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF007AFF).copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.4f)
        ),
        enabled = isEnabled,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(
            text = stringResource(id = text),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
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
