package io.anyline.tiretread.demo.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.github.barteksc.pdfviewer.util.FitPolicy
import io.anyline.tiretread.demo.Response
import io.anyline.tiretread.demo.databinding.ActivityMeasurementResultDetailsBinding
import java.io.File

class MeasurementResultDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeasurementResultDetailsBinding
    private val viewModel: MeasurementResultDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasurementResultDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra(EXTRA_UUID)?.let { uuid ->
            observePdfByteStream()
            observeFile()

            binding.btDetailsOK.setOnClickListener {
                finish()
            }

            binding.btPdfDownload.setOnClickListener {
                viewModel.saveToFile()
            }

            viewModel.loadPdf(uuid)
        } ?: finish()
    }

    private fun observePdfByteStream() {
        viewModel.pdfByteStream.observe(this) {
            when (it) {
                is Response.Success -> {
                    binding.progressBar.visibility = GONE

                    binding.pdfViewer.minZoom = 0.4f
                    binding.pdfViewer
                        .fromBytes(it.result)
                        .load()
                    binding.pdfViewer.zoomTo(0.8f)
                }

                is Response.Error -> {
                    binding.progressBar.visibility = GONE
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }

                is Response.Loading -> {
                    binding.progressBar.visibility = VISIBLE
                }
            }

        }
    }

    private fun observeFile() {
        viewModel.file.observe(this) {
            when (it) {
                is Response.Success -> {
                    binding.progressBar.visibility = GONE
                    showSavePdfIntent(it.result)
                }

                is Response.Loading -> {
                    binding.progressBar.visibility = VISIBLE
                }

                is Response.Error -> {
                    binding.progressBar.visibility = GONE
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showSavePdfIntent(it: File) {
        val browserIntent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(
            applicationContext, "$packageName.provider", it
        )
        browserIntent.setDataAndType(uri, "application/pdf")
        browserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(browserIntent, "Open with")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(chooser)
    }

    companion object {

        private const val EXTRA_UUID = "EXTRA_UUID"

        fun newIntent(context: Context, uuid: String): Intent {

            return Intent(context, MeasurementResultDetailsActivity::class.java).apply {
                putExtra(EXTRA_UUID, uuid)
            }
        }
    }
}