package io.anyline.tiretread.devexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.toJson
import org.json.JSONObject

class ResultActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        intent.getStringExtra("uuid")?.let { uuid ->
            title = "Results: $uuid"
            setContent { ResultScreen(viewModel, uuid) }
            viewModel.fetchResults(uuid)
        } ?: finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    @Composable
    fun ResultScreen(viewModel: ResultViewModel, measurementUuid: String) {
        val treadDepthResult by viewModel.treadDepthResult
        val errorMessage by viewModel.errorMessage

        when {
            treadDepthResult != null -> {
                ResultContent(treadDepthResult!!, measurementUuid)
            }

            errorMessage != null -> {
                ErrorContent("", errorMessage!!)
            }

            else -> {
                LoadingView()
            }
        }
    }

    @Composable
    fun LoadingView() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    @Composable
    fun ErrorContent(code: String, message: String) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error: $code: $message",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 100.dp)
                )
            }
        }
    }

    @SuppressLint("DefaultLocale")
    @Composable
    fun ResultContent(results: TreadDepthResult, measurementUuid: String) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Global value
            Text(
                text = String.format("%.2f", results.global.valueMm),
                color = if (isSystemInDarkTheme()) Color.Black else Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                    .padding(10.dp)
            )

            // Minimum value
            Text(
                text = String.format("%.2f", results.minimumValue.valueMm),
                color = if (isSystemInDarkTheme()) Color.Black else Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                    .padding(10.dp)
            )

            // Heatmap
            HeatMapView(measurementUuid)

            // Regional values
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                results.regions.forEach { region ->
                    Text(
                        text = String.format("%.2f", region.valueMm),
                        color = if (isSystemInDarkTheme()) Color.Black else Color.White,
                        fontSize = 25.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                            .padding(10.dp)
                    )
                }
            }

            // Full JSON result
            val jsonObject = JSONObject(results.toJson())
            Card(
                border = BorderStroke(2.dp, Color.Gray),
                backgroundColor = Color.Transparent,
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = jsonObject.toString(4),
                    fontSize = 18.sp,
                    color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }

    @Composable
    fun HeatMapView(measurementUuid: String) {
        val heatmapState = viewModel.heatmap

        LaunchedEffect(measurementUuid) {
            viewModel.fetchHeatmap(measurementUuid)
        }

        val imageLoader = ImageLoader.Builder(this@ResultActivity).build()

        heatmapState.value?.let {
            AsyncImage(
                imageLoader = imageLoader,
                model = heatmapState.value?.url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(200.dp)
                    .shadow(1.dp)
            )
        }
    }

}