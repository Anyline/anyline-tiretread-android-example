package io.anyline.tiretread.devexample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import io.anyline.tiretread.devexample.ui.theme.TTRDeveloperExamplesTheme

/**
 * Displays a loading indicator centered in the screen.
 * Prevents touches to the background.
 *
 * @param isBusy If true, displays the loading indicator. Displays nothing otherwise.
 */
@Composable
fun DevExLoadingView(isBusy: Boolean) {
    if (isBusy) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) { // Intercept clicks without animation
                    detectTapGestures(onTap = {})
                },
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(0.15f)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun LoadingViewPreview() {
    TTRDeveloperExamplesTheme {
        DevExLoadingView(isBusy = true)
    }
}