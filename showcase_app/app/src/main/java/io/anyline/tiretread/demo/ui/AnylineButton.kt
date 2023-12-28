package io.anyline.tiretread.demo.ui

import android.content.Context
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import io.anyline.tiretread.demo.R

@Composable
internal fun AnylineButton(
    context: Context,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colorAnylineBlue = with(context.getColor(R.color.primary_anyline)) {
        Color(red, green, blue, alpha)
    }
    val colorWhite = with(context.getColor(R.color.white)) {
        Color(red, green, blue, alpha)
    }


    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colorAnylineBlue),
        shape = RoundedCornerShape(24.dp, 0.dp, 0.dp, 24.dp),
        modifier = Modifier
            .then(modifier)
            .width(125.dp)
            .height(60.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 14.sp,
            color = colorWhite,
            fontFamily = FontFamily(Font(R.font.proxima_nova_bold))
        )
    }
}