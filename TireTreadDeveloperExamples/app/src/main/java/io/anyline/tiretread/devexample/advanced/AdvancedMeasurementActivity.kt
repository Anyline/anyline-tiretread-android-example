package io.anyline.tiretread.devexample.advanced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.devexample.MainApplication
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.common.ScanTireTreadActivity
import io.anyline.tiretread.devexample.common.ScanTireTreadViewModel
import io.anyline.tiretread.devexample.config.SelectConfigContent
import io.anyline.tiretread.devexample.config.SelectConfigFragment
import io.anyline.tiretread.devexample.config.ValidationResult
import io.anyline.tiretread.devexample.databinding.ActivityNewMeasurementBinding

class AdvancedMeasurementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewMeasurementBinding
    private lateinit var selectConfigFragment: SelectConfigFragment

    private val getScanTireTreadActivityResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(
                    this,
                    "Scan process is complete. You will receive a notification when image processing is finished.",
                    Toast.LENGTH_LONG
                )
                    .show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMeasurementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        startNotificationService()

        selectConfigFragment = SelectConfigFragment.newInstance(
            selectConfigContent = SelectConfigContent.DefaultConfigWithTireWidthContent,
            onStartScanButtonClick = { selectConfigFragmentValidation ->
                validate(selectConfigFragmentValidation).also { validationResult ->
                    when (validationResult) {
                        is AdvancedMeasurementValidationResult.Succeed -> {
                            val intent = ScanTireTreadActivity.buildIntent(
                                context = this,
                                ScanTireTreadActivity.ScanTireTreadActivityParameters(
                                    configContent = validationResult.selectConfigValidation.configFileContent,
                                    scanSpeed = validationResult.selectConfigValidation.scanSpeed,
                                    measurementSystem = validationResult.selectConfigValidation.measurementSystem,
                                    tireWidth = validationResult.selectConfigValidation.tireWidth,
                                    showGuidance = validationResult.selectConfigValidation.showGuidance,
                                    customData = MeasurementResultCustomData(
                                        validationResult.description,
                                        validationResult.position
                                    ).toString(),
                                    scopeStrategy = ScanTireTreadViewModel.ScopeStrategy.CaptureAndUploadOnly
                                ),
                                measurementResultUpdateInterface = (application as MainApplication).notificationService
                            )

                            getScanTireTreadActivityResult.launch(intent)
                        }

                        is AdvancedMeasurementValidationResult.Failed -> {
                            showAlertDialog(getString(R.string.app_name), validationResult.message)
                        }
                    }
                }
            })
        binding.fragmentContainerSelectConfig.apply {
            supportFragmentManager.beginTransaction()
                .add(this.id, selectConfigFragment)
                .commit()
        }

        with(binding.positionAutocompleteEditText) {
            setAdapter(
                ArrayAdapter(
                    this@AdvancedMeasurementActivity,
                    android.R.layout.select_dialog_item,
                    positions
                )
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    sealed class AdvancedMeasurementValidationResult {
        data class Succeed(
            val selectConfigValidation: ValidationResult.Succeed,
            val description: String,
            val position: String
        ) : AdvancedMeasurementValidationResult()

        data class Failed(val message: String) : AdvancedMeasurementValidationResult()
    }

    private fun validate(selectConfigFragmentValidation: ValidationResult)
            : AdvancedMeasurementValidationResult {
        return when (selectConfigFragmentValidation) {
            is ValidationResult.Succeed -> {
                AdvancedMeasurementValidationResult.Succeed(
                    selectConfigFragmentValidation,
                    binding.descriptionEditText.text.toString(),
                    binding.positionAutocompleteEditText.text.toString()
                )
            }

            is ValidationResult.Failed -> {
                AdvancedMeasurementValidationResult.Failed(selectConfigFragmentValidation.message)
            }
        }
    }

    private fun startNotificationService() {
        Intent(this, NotificationService::class.java).apply {
            startService(this)
        }
    }

    private fun showAlertDialog(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        runOnUiThread {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setOnDismissListener { onDismiss?.invoke() }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
    }

    companion object {
        private val positions = arrayOf("Front Left", "Front Right", "Rear Left", "Rear Right")

        fun buildIntent(context: Context): Intent {
            return Intent(context, AdvancedMeasurementActivity::class.java)
        }
    }
}