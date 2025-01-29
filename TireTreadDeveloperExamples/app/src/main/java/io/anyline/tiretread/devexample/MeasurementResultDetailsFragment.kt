package io.anyline.tiretread.devexample

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.anyline.tiretread.devexample.common.MeasurementResultData
import io.anyline.tiretread.devexample.common.MeasurementResultDetails
import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import io.anyline.tiretread.devexample.databinding.FragmentMeasurementResultDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MeasurementResultDetailsFragment: Fragment() {
    private lateinit var binding: FragmentMeasurementResultDetailsBinding
    private var measurementResultDetails: MeasurementResultDetails? = null
    var measurementResultData: MeasurementResultData? = null
        set(value) {
            value?.let { measurementResultData ->
                if (measurementResultData.measurementResultStatus is MeasurementResultStatus.TreadDepthResultQueried) {
                    measurementResultDetails = MeasurementResultDetails(measurementResultData).apply {
                        showResultDetails(this)
                    }
                }
                else {
                    hideResultDetails()
                }
            }
            field = value
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasurementResultDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.heatMapRetryButton) {
            setOnClickListener {
                loadHeatMapImage()
            }
        }
    }

    private fun showResultDetails(details: MeasurementResultDetails) {
        binding.mainResultLayout.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.Main) {
            details.heatMapResultValue.collect { heatMapState ->
                when (heatMapState) {
                    is MeasurementResultDetails.HeatMapState.Unknown,
                    is MeasurementResultDetails.HeatMapState.GettingHeatMapUrl,
                    is MeasurementResultDetails.HeatMapState.HeatMapUrlReady,
                    is MeasurementResultDetails.HeatMapState.DownloadingHeatMap -> {
                        binding.heatMapAvailableLayout.visibility = View.GONE
                        binding.heatMapLoadingLayout.visibility = View.VISIBLE
                        binding.heatMapProgressBar.visibility = View.VISIBLE
                        binding.heatMapRetryButton.visibility = View.GONE
                        binding.heatMapTextView.text = "Loading Heat Map..."
                    }
                    is MeasurementResultDetails.HeatMapState.Failed -> {
                        binding.heatMapAvailableLayout.visibility = View.GONE
                        binding.heatMapLoadingLayout.visibility = View.VISIBLE
                        binding.heatMapProgressBar.visibility = View.GONE
                        binding.heatMapRetryButton.visibility = View.VISIBLE
                        binding.heatMapTextView.text = heatMapState.message
                    }
                    is MeasurementResultDetails.HeatMapState.Ready -> {
                        binding.heatMapAvailableLayout.visibility = View.VISIBLE
                        binding.heatMapLoadingLayout.visibility = View.GONE
                        binding.heatMapImageView.setImageBitmap(BitmapFactory.decodeStream(heatMapState.stream))
                        binding.heatMapImageView.setOnClickListener {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(heatMapState.heatmap.url)))
                        }
                    }
                }
            }
        }
        loadHeatMapImage()
    }

    private fun hideResultDetails() {
        binding.mainResultLayout.visibility = View.GONE
    }

    private fun loadHeatMapImage() {
        lifecycleScope.launch(Dispatchers.IO)  {
            measurementResultDetails?.getHeatMap()
        }
    }

}