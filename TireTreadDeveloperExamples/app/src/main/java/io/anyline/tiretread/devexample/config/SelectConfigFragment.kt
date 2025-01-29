package io.anyline.tiretread.devexample.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import io.anyline.tiretread.devexample.databinding.FragmentSelectConfigBinding
import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.scanner.ScanSpeed
import io.anyline.tiretread.sdk.scanner.TireTreadScanViewConfig
import kotlinx.serialization.json.Json

class SelectConfigFragment(): Fragment() {
    private lateinit var binding: FragmentSelectConfigBinding

    private val configFiles: Array<String> by lazy {
        requireContext().assets.list(ASSETS_FOLDER) ?: arrayOf()
    }

    private var configFileContent: String? = null

    private var selectConfigContent: SelectConfigContent = SelectConfigContent.DefaultConfigContent
    private var onStartScanButtonClick: ((ValidationResult) -> Unit)? = null

    internal constructor(
        selectConfigContent: SelectConfigContent,
        onStartScanButtonClick: ((ValidationResult) -> Unit)?) : this() {
        this.selectConfigContent = selectConfigContent
        this.onStartScanButtonClick = onStartScanButtonClick
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.configFileSpinner) {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                configFiles
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            onItemSelectedListener = ConfigFileOnItemSelectedListener()
        }

        if (selectConfigContent.containsConfigFile) {
            binding.configFileContentLayout.visibility = View.VISIBLE
        }
        else {
            binding.configFileContentLayout.visibility = View.GONE
        }

        if (selectConfigContent.containsScanSpeed) {
            binding.otherConfigScanSpeedContentLayout.visibility = View.VISIBLE
            binding.otherConfigScanSpeedMaterialSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.otherConfigScanSpeedTextView.isEnabled = isChecked
                binding.otherConfigScanSpeedFastRadiobutton.isEnabled = isChecked
                binding.otherConfigScanSpeedSlowRadiobutton.isEnabled = isChecked
            }
        }
        else {
            binding.otherConfigScanSpeedContentLayout.visibility = View.GONE
        }

        if (selectConfigContent.containsMeasurementSystem) {
            binding.otherConfigUnitsContentLayout.visibility = View.VISIBLE
            binding.otherConfigUnitsMaterialSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.otherConfigUnitsTextView.isEnabled = isChecked
                binding.otherConfigUnitsMetricRadiobutton.isEnabled = isChecked
                binding.otherConfigUnitsImperialRadiobutton.isEnabled = isChecked
            }
        }
        else {
            binding.otherConfigUnitsContentLayout.visibility = View.GONE
        }

        if (selectConfigContent.containsTireWidth) {
            binding.otherConfigWidthContentLayout.visibility = View.VISIBLE
            binding.otherConfigWidthMaterialSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.otherConfigWidthTextView.isEnabled = isChecked
                binding.otherConfigWidthAutocompleteEditText.isEnabled = isChecked
            }

            with (binding.otherConfigWidthAutocompleteEditText) {
                setAdapter(
                    ArrayAdapter(requireContext(),
                        android.R.layout.select_dialog_item,
                        validTireWidths
                    ))

                setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        (view as AutoCompleteTextView).showDropDown()
                    }
                }
            }
        }
        else {
            binding.otherConfigWidthContentLayout.visibility = View.GONE
        }

        if (selectConfigContent.containsShowGuidance) {
            binding.otherConfigUiContentLayout.visibility = View.VISIBLE
            binding.otherConfigUiMaterialSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.otherConfigUiTextView.isEnabled = isChecked
            }
            binding.otherConfigUiMaterialSwitch.isChecked = true
        }
        else {
            binding.otherConfigUiContentLayout.visibility = View.GONE
        }

        onStartScanButtonClick?.let { onStartScanButtonClick ->
            binding.scanNowButton.setOnClickListener {
                onStartScanButtonClick.invoke(validate())
            }
        }
    }

    inner class ConfigFileOnItemSelectedListener: OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            configFileContent = null
            val clickedConfigText = parent.getItemAtPosition(pos)
            val configFileName = "$ASSETS_FOLDER/$clickedConfigText"
            configFileContent = requireContext().assets.open(configFileName).bufferedReader().use {
                it.readText()
            }.also { configContent ->
                val scanConfig = Json.decodeFromString<TireTreadScanViewConfig>(configContent)
                if (selectConfigContent.containsScanSpeed) {
                    when (scanConfig.scanSpeed) {
                        ScanSpeed.Fast -> binding.otherConfigScanSpeedFastRadiobutton.isChecked = true
                        ScanSpeed.Slow -> binding.otherConfigScanSpeedSlowRadiobutton.isChecked = true
                    }
                }
                if (selectConfigContent.containsMeasurementSystem) {
                    when (scanConfig.measurementSystem) {
                        MeasurementSystem.Metric -> binding.otherConfigUnitsMetricRadiobutton.isChecked = true
                        MeasurementSystem.Imperial -> binding.otherConfigUnitsImperialRadiobutton.isChecked = true
                    }
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // Another interface callback.
        }
    }

    private fun validate(): ValidationResult {
        var configFileContent: String? = null
        if (selectConfigContent.containsConfigFile) {
            if (this.configFileContent == null) {
                return ValidationResult.Failed(selectConfigContent, "You must select a Config file.")
            }
            configFileContent = this.configFileContent
        }

        var scanSpeed: ScanSpeed? = null
        if (selectConfigContent.containsScanSpeed && binding.otherConfigScanSpeedMaterialSwitch.isChecked) {
            if (binding.otherConfigScanSpeedFastRadiobutton.isChecked) {
                scanSpeed = ScanSpeed.Fast
            }
            if (binding.otherConfigScanSpeedSlowRadiobutton.isChecked) {
                scanSpeed = ScanSpeed.Slow
            }
        }

        var measurementSystem: MeasurementSystem? = null
        if (selectConfigContent.containsMeasurementSystem && binding.otherConfigUnitsMaterialSwitch.isChecked) {
            if (binding.otherConfigUnitsMetricRadiobutton.isChecked) {
                measurementSystem = MeasurementSystem.Metric
            }
            if (binding.otherConfigUnitsImperialRadiobutton.isChecked) {
                measurementSystem = MeasurementSystem.Imperial
            }
        }

        var tireWidth: Int? = null
        if (selectConfigContent.containsTireWidth && binding.otherConfigWidthMaterialSwitch.isChecked) {
            if (!validTireWidths.contains(binding.otherConfigWidthAutocompleteEditText.text.toString())) {
                return ValidationResult.Failed(selectConfigContent, "Invalid Tire Width!")
            }
            tireWidth = Integer.parseInt(binding.otherConfigWidthAutocompleteEditText.text.toString())
        }

        var showGuidance: Boolean? = null
        if (selectConfigContent.containsShowGuidance) {
            showGuidance = binding.otherConfigUiMaterialSwitch.isChecked
        }

        return ValidationResult.Succeed(
            selectConfigContent,
            configFileContent,
            scanSpeed,
            measurementSystem,
            tireWidth,
            showGuidance)
    }

    companion object {
        private const val ASSETS_FOLDER = "tire-tread-configs"

        private val validTireWidths: Array<String> = mutableListOf<String>().apply {
            for (i in 105..495 step 5) {
                add(i.toString())
            }
        }.toTypedArray()

        fun newInstance(
            selectConfigContent: SelectConfigContent,
            onStartScanButtonClick: ((ValidationResult) -> Unit)): SelectConfigFragment {
            return SelectConfigFragment(selectConfigContent, onStartScanButtonClick)
        }
    }

}