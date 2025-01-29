package io.anyline.tiretread.devexample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import io.anyline.tiretread.devexample.advanced.AdvancedMeasurementActivity
import io.anyline.tiretread.devexample.advanced.MeasurementResultListActivity
import io.anyline.tiretread.devexample.config.SelectConfigContent
import io.anyline.tiretread.devexample.config.SelectConfigDialogFragment
import io.anyline.tiretread.devexample.config.SelectConfigFragment
import io.anyline.tiretread.devexample.config.ValidationResult
import io.anyline.tiretread.devexample.databinding.ActivityMainBinding
import io.anyline.tiretread.devexample.simple.SimpleMeasurementActivity
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.NoConnectionException
import io.anyline.tiretread.sdk.SdkLicenseKeyForbiddenException
import io.anyline.tiretread.sdk.SdkLicenseKeyInvalidException
import io.anyline.tiretread.sdk.init

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 100

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private val postNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startActivity(AdvancedMeasurementActivity.buildIntent(this))
            }
        }

    private var isInitialized: Boolean = false
    private var initializationError: String? = null

    private val fragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            if (className == SelectConfigDialogFragment::class.java.name) {
                return SelectConfigDialogFragment(
                    mainViewModel.lastSelectConfigContent.value!!,
                    mainViewModel.lastOnSelectConfigDialogFragmentButton.value!!
                )
            } else if (className == SelectConfigFragment::class.java.name) {
                return SelectConfigFragment.newInstance(
                    mainViewModel.lastSelectConfigContent.value!!,
                    mainViewModel.lastOnSelectConfigDialogFragmentButton.value!!
                )
            }
            val fragment = super.instantiate(classLoader, className)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = fragmentFactory
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        isInitialized = when (AnylineTireTreadSdk.isInitialized) {
            true -> true
            false -> {
                initializeAnylineTireTreadSdk { error ->
                    initializationError = error
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Check for Camera Permission
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }

        updateUi()

        // Simple implementation of the Tire Tread Scanner, using Compose
        binding.composeActivityDefaultConfigScanButton.setOnClickListener {
            startActivity(Intent(this, ComposeScanActivity::class.java))
        }

        // Simple implementation of the Tire Tread Scanner, using XML Layout
        binding.xmlActivityDefaultConfigScanButton.setOnClickListener {
            startActivity(Intent(this, XmlScanActivity::class.java))
        }

        // Example allowing to customize the properties of a TireTreadScanViewConfig object
        binding.manualConfigScanButton.setOnClickListener {
            requestScanConfig(SelectConfigContent.ManualConfigContent) { validationResult ->
                when (validationResult) {
                    is ValidationResult.Succeed -> {
                        startActivity(SimpleMeasurementActivity.buildIntent(this, validationResult))
                    }

                    is ValidationResult.Failed -> {
                        showAlertDialog(getString(R.string.app_name), validationResult.message)
                    }
                }
            }
        }

        // Example using JSON files to configure the Tire Tread Scanner
        binding.jsonConfigScanButton.setOnClickListener {
            requestScanConfig(SelectConfigContent.JsonConfigContent) { validationResult ->
                when (validationResult) {
                    is ValidationResult.Succeed -> {
                        startActivity(SimpleMeasurementActivity.buildIntent(this, validationResult))
                    }

                    is ValidationResult.Failed -> {
                        showAlertDialog(getString(R.string.app_name), validationResult.message)
                    }
                }
            }
        }

        // Advanced examples, demonstrating how to run async scans, for a more dynamic workflow
        binding.asyncConfigScanButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startActivity(AdvancedMeasurementActivity.buildIntent(this))
            }
        }

        // Example on how to store & load previous measurement results
        binding.resultsButton.setOnClickListener {
            startActivity(MeasurementResultListActivity.buildIntent(this))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@MainActivity,
                    "Permission granted. You can open the scanner now.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(
                    this@MainActivity,
                    "Permission denied. You won't be able to use the scanner",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    private fun initializeAnylineTireTreadSdk(onError: ((String?) -> Unit)): Boolean {
        // Try/Catch this to check if your license key is valid or not.
        try {
            //replace BuildConfig.LICENSE_KEY on line below with your license key
            val licenseKey = BuildConfig.LICENSE_KEY
            AnylineTireTreadSdk.init(licenseKey, applicationContext)
            return true
        } catch (e: SdkLicenseKeyInvalidException) {
            onError.invoke(e.message)
        } catch (e: SdkLicenseKeyForbiddenException) {
            onError.invoke(e.message)
        } catch (e: NoConnectionException) {
            onError.invoke(e.message)
        } catch (e: Exception) {
            onError.invoke(e.message)
        }
        return false
    }

    private fun updateUi() {
        if (AnylineTireTreadSdk.isInitialized) {
            binding.versionTextview.text =
                "Anyline TireTread SDK Version: ${AnylineTireTreadSdk.sdkVersion}"
        } else {
            binding.versionTextview.text = initializationError
            binding.composeActivityDefaultConfigScanButton.isEnabled = false
            binding.xmlActivityDefaultConfigScanButton.isEnabled = false
            binding.manualConfigScanButton.isEnabled = false
            binding.jsonConfigScanButton.isEnabled = false
            binding.asyncConfigScanButton.isEnabled = false
            binding.resultsButton.isEnabled = false
        }
    }

    private fun requestScanConfig(
        selectConfigContent: SelectConfigContent,
        onSelectConfigDialogFragmentButton: ((ValidationResult) -> Unit)
    ) {

        mainViewModel.lastSelectConfigContent.postValue(selectConfigContent)
        mainViewModel.lastOnSelectConfigDialogFragmentButton.postValue(
            onSelectConfigDialogFragmentButton
        )

        if (selectConfigContent.hasContentToValidate()) {
            SelectConfigDialogFragment(
                selectConfigContent,
                onSelectConfigDialogFragmentButton
            ).also { selectConfigDialogFragment ->
                selectConfigDialogFragment.show(
                    supportFragmentManager,
                    SelectConfigDialogFragment.TAG
                )
            }
        } else {
            val autoValidationResult = ValidationResult.Succeed(selectConfigContent)
            onSelectConfigDialogFragmentButton.invoke(autoValidationResult)
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
}