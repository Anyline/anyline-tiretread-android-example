package io.anyline.tiretread.devexample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.anyline.tiretread.devexample.common.MeasurementResultData
import io.anyline.tiretread.devexample.databinding.FragmentMeasurementResultBinding
import io.anyline.tiretread.devexample.databinding.FragmentRegionResultBinding
import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion

class MeasurementResultFragment: Fragment() {
    private lateinit var binding: FragmentMeasurementResultBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasurementResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun displayMeasurementResult(
        measurementResultData: MeasurementResultData,
        treadDepthResult: TreadDepthResult) {
        with(binding) {
            measurementResultsLayout.visibility = View.VISIBLE

            // Display the Global Result
            if (measurementResultData.measurementSystem == MeasurementSystem.Imperial) {
                val globalResultInch32nds = treadDepthResult.global.valueInch32nds

                lineInchesDividerView.visibility = View.VISIBLE

                resultInch32ndsGlobalTextView.text = globalResultInch32nds.toString()
                denominatorGlobalTextView.visibility = View.VISIBLE
            } else {
                val globalResultMillimeter = treadDepthResult.global.valueMm
                resultGlobalTextView.text = String.format("%.1f\nmm", globalResultMillimeter)
            }

            // Clean the region results
            measurementResultRegionsLayout.removeAllViews()

            // Divide the layout by the total of regions and spaces around them.
            measurementResultRegionsLayout.weightSum = treadDepthResult.regions.size.toFloat() * 2

            // Display the regions dynamically, from left to right.
            for (region in treadDepthResult.regions) {
                if (region.isAvailable) measurementResultRegionsLayout.addView(
                    createAvailableRegionResultView(measurementResultData, region)
                )
                else measurementResultRegionsLayout.addView(createUnavailableRegionResultView())
            }
        }
    }

    fun hideMeasurementResult() {
        binding.measurementResultsLayout.visibility = View.GONE
    }

    private fun createAvailableRegionResultView(
        measurementResultData: MeasurementResultData,
        region: TreadResultRegion): View {
        val fragmentBinding = FragmentRegionResultBinding.inflate(layoutInflater)
        val llRegionResult = fragmentBinding.regionResultLayout

        if (measurementResultData.measurementSystem == MeasurementSystem.Imperial) {
            fragmentBinding.lineInchesDividerView.visibility = View.VISIBLE

            fragmentBinding.result32ndsInchTextView.text = region.valueInch32nds.toString()
            fragmentBinding.regionResultDenominatorInchTextView.visibility = View.VISIBLE
        } else {
            fragmentBinding.regionResultMillimeterTextView.text =
                String.format("%.1f", region.valueMm) + "\nmm"
        }
        return fragmentBinding.root
    }

    private fun createUnavailableRegionResultView(): View {
        val fragmentBinding = FragmentRegionResultBinding.inflate(layoutInflater)
        val tvRegionResult = fragmentBinding.regionResultMillimeterTextView
        tvRegionResult.text = "-"
        return fragmentBinding.root
    }
}