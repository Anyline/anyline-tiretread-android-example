package io.anyline.tiretread.demo.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.demo.databinding.ActivityTireWidthBinding

class TireWidthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTireWidthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTireWidthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            buttonCancel.setOnClickListener {
                finish()
            }

            buttonSkip.setOnClickListener {
                goToScanActivity()
            }

            buttonOk.setOnClickListener {
                val tireWidthText = binding.editTextTireWidth.text.toString()

                if (tireWidthText.isEmpty()) {
                    binding.editTextTireWidth.error = "Please enter a valid tire width."
                    return@setOnClickListener
                }

                val tireWidth: Int = try {
                    tireWidthText.toInt()
                } catch (e: Exception) {
                    binding.editTextTireWidth.error = "Please enter a valid tire width."
                    return@setOnClickListener
                }

                if (tireWidth <= 100 || tireWidth >= 500) {
                    binding.editTextTireWidth.error = "Please enter a value between 100 and 500."
                    return@setOnClickListener
                } else if (tireWidth % 5 != 0) {
                    binding.editTextTireWidth.error = "Please enter a multiple of 5."
                    return@setOnClickListener
                }
                goToScanActivity(tireWidth)
            }
        }
    }

    private fun goToScanActivity(tireWidth: Int? = null) {
        val intent = Intent(this@TireWidthActivity, ScanActivity::class.java)
        intent.putExtra("tireWidth", tireWidth)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}