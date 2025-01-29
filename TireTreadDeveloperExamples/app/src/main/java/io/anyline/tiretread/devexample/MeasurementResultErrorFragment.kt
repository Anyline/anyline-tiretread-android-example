package io.anyline.tiretread.devexample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import io.anyline.tiretread.devexample.common.TreadDepthResultStatus
import io.anyline.tiretread.devexample.databinding.FragmentMeasurementResultErrorBinding


class MeasurementResultErrorFragment: Fragment() {
    private lateinit var binding: FragmentMeasurementResultErrorBinding

    var measurementResultStatus: MeasurementResultStatus? = null
        set(value) {
            value?.let { measurementResultStatus ->
                checkStatusForError(measurementResultStatus)
            }
            field = value
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasurementResultErrorBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun checkStatusForError(status: MeasurementResultStatus) {
        when (status) {
            is MeasurementResultStatus.ScanAborted -> {
                showError(status.statusDescription, "")
            }
            is MeasurementResultStatus.Error -> {
                showError(status.statusDescription, status.reason)
            }
            is MeasurementResultStatus.TreadDepthResultQueried -> {
                status.treadDepthResultStatus.also { treadDepthResultStatus ->
                    when (treadDepthResultStatus) {
                        is TreadDepthResultStatus.NotYetAvailable -> {
                            showError(treadDepthResultStatus.statusDescription, "")
                        }
                        is TreadDepthResultStatus.Failed -> {
                            showError(treadDepthResultStatus.statusDescription, treadDepthResultStatus.reason)
                        }
                        else -> {
                            hideError()
                        }
                    }
                }
            }
            else -> {
                hideError()
            }
        }
    }

    fun showError(title: String, message: String?, enableRetry: (() -> Unit)? = null) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorTitleTextView.text = title
        binding.errorMessageTextView.text = message
    }

    fun hideError() {
        binding.errorLayout.visibility = View.GONE
    }
}