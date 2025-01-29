package io.anyline.tiretread.devexample.advanced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.anyline.tiretread.devexample.R
import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import io.anyline.tiretread.devexample.databinding.ActivityMeasurementResultListBinding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeasurementResultListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeasurementResultListBinding

    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO
    private val dispatcherMain: CoroutineDispatcher = Dispatchers.Main
    private val loadingFilesState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var rootMeasurementResultFolder: MeasurementResultFolder? = null
    private var currentMeasurementResultFolder: MeasurementResultFolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementResultListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.swiperefresh.apply {
            isEnabled = false
            lifecycleScope.launch {
                loadingFilesState.collect { loadingState ->
                    isRefreshing = loadingState
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (loadingFilesState.value) {
                    return
                }
                currentMeasurementResultFolder?.parent?.let {
                    navigateToFolder(it)
                    return
                }
                finish()
            }
        })

        binding.viewConfigs.layoutManager = LinearLayoutManager(this@MeasurementResultListActivity)
        rootMeasurementResultFolder?.let {
            navigateToFolder(it)
        } ?: run {
            loadFromFolder() { scanViewConfigFolder ->
                navigateToFolder(scanViewConfigFolder)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(Menu.NONE, VIEW_OPTIONS_MENU_ID, Menu.NONE,
            R.string.select_measurement_result_view_options
        )
            ?.setIcon(R.drawable.ic_action_filter)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        else if (item.itemId == VIEW_OPTIONS_MENU_ID) {
            showViewOptionsDialog()
        }
        return false
    }

    private fun loadFromFolder(onFinish: ((MeasurementResultFolder) -> Unit)) {
        loadingFilesState.update { true }
        lifecycleScope.launch(dispatcherIO) {
            rootMeasurementResultFolder = MeasurementResultFolder.loadFromFolder(
                this@MeasurementResultListActivity,
                MeasurementResultFolder.getOrCreateResultsFolder(this@MeasurementResultListActivity),
                "Measurement Results",
                listOptions
            )
            withContext(dispatcherMain) {
                currentMeasurementResultFolder = rootMeasurementResultFolder
                loadingFilesState.update { false }
                currentMeasurementResultFolder?.let {
                    onFinish.invoke(it)
                }
            }
        }
    }

    private fun navigateToFolder(scanViewConfigFolder: MeasurementResultFolder) {
        currentMeasurementResultFolder = scanViewConfigFolder
        binding.viewConfigs.adapter = ConfigRecyclerViewAdapter(
            scanViewConfigList = scanViewConfigFolder.asOrderedList(),
            onFolderSelected = ::navigateToFolder,
            onViewConfigSelected = ::onFileSelected,
            onViewConfigHelpRequested = ::onFileHelpSelected)
        title = scanViewConfigFolder.friendlyName
    }

    private fun onFileSelected(measurementResultFile: MeasurementResultFile) {
        with (measurementResultFile.fileData.measurementResultData) {
            if (measurementResultStatus is MeasurementResultStatus.TreadDepthResultQueried) {
                val intent = MeasurementResultActivity
                    .buildIntent(this@MeasurementResultListActivity, this)
                startActivity(intent)
            }
        }
    }

    private fun onFileHelpSelected(measurementResultFile: MeasurementResultFile) {
        //no implementation needed
    }

    private fun showAlertDialog(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
            .setMessage(message)
            .setOnDismissListener { onDismiss?.invoke() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun showViewOptionsDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.select_measurement_result_view_options)
            .setSingleChoiceItems(
                MeasurementResultFolder.ListOptions.asDescriptionList(),
                listOptions.ordinal
            ) { dialogInterface, intChoice ->
                listOptions = MeasurementResultFolder.ListOptions.entries.toTypedArray()[intChoice]
                loadFromFolder() { scanViewConfigFolder ->
                    navigateToFolder(scanViewConfigFolder)
                }
                dialogInterface.dismiss()
            }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    class ConfigRecyclerViewAdapter(
        private val scanViewConfigList: List<Any>,
        private val onFolderSelected: (MeasurementResultFolder) -> Unit,
        private val onViewConfigSelected: (MeasurementResultFile) -> Unit,
        private val onViewConfigHelpRequested: (MeasurementResultFile) -> Unit
    ) : RecyclerView.Adapter<ConfigRecyclerViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ConfigRecyclerViewHolder {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_view_select_measurement,
                    parent,
                    false
                )
            return ConfigRecyclerViewHolder(view,
                onFolderClick = onFolderSelected,
                onFileClick = onViewConfigSelected,
                onFileHelpClick = onViewConfigHelpRequested)
        }

        override fun onBindViewHolder(holder: ConfigRecyclerViewHolder, position: Int) {
            holder.bind(scanViewConfigList[position])
        }

        override fun getItemCount(): Int = scanViewConfigList.size
    }

    class ConfigRecyclerViewHolder(
        private val item: View,
        private val onFolderClick: (MeasurementResultFolder) -> Unit,
        private val onFileClick: (MeasurementResultFile) -> Unit,
        private val onFileHelpClick: (MeasurementResultFile) -> Unit
    ) : RecyclerView.ViewHolder(item) {

        fun bind(viewConfigFileOrFolder: Any) {
            if (viewConfigFileOrFolder is MeasurementResultFile) {
                item.findViewById<ImageView>(R.id.view_image).apply {
                    setImageResource(R.drawable.ic_notification_tire)
                }
                item.findViewById<TextView>(R.id.view_name).apply {
                    text = viewConfigFileOrFolder.fileData.getCaption()
                }
                item.findViewById<TextView>(R.id.view_description_text).apply {
                    text = viewConfigFileOrFolder.fileData.measurementResultData.measurementResultStatus.statusDescription
                }
                item.findViewById<ImageView>(R.id.view_help_image).apply {
                    setOnClickListener(null)
                    visibility = View.GONE
                }
                item.setOnClickListener { onFileClick(viewConfigFileOrFolder) }
            }
            else if (viewConfigFileOrFolder is MeasurementResultFolder) {
                item.findViewById<ImageView>(R.id.view_image).apply {
                    setImageResource(R.drawable.ic_folder)
                }
                item.findViewById<TextView>(R.id.view_name).apply {
                    text = viewConfigFileOrFolder.friendlyName
                }
                item.findViewById<TextView>(R.id.view_description_text).apply {
                    text = ""
                }
                item.setOnClickListener { onFolderClick(viewConfigFileOrFolder) }
            }
        }
    }

    companion object {
        private const val VIEW_OPTIONS_MENU_ID = 1

        var listOptions: MeasurementResultFolder.ListOptions =
            MeasurementResultFolder.ListOptions.OriginalStructure

        fun buildIntent(context: Context): Intent {
            return Intent(context, MeasurementResultListActivity::class.java)
        }
    }
}