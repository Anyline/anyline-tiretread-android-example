package io.anyline.tiretread.demo.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.anyline.tiretread.demo.R
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.sdk.AnylineTireTreadSdk
import io.anyline.tiretread.sdk.sendCommentFeedback
import io.anyline.tiretread.sdk.sendTreadDepthResultFeedback
import io.anyline.tiretread.sdk.types.TreadDepthResult
import io.anyline.tiretread.sdk.types.TreadResultRegion
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResultFeedbackActivity : Activity() {
    private var measurementUuid: String = ""
    private lateinit var measurementResult: TreadDepthResult
    private val editTextsList = arrayListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_feedback)

        // Get UUID from bundle
        val bundle = intent.extras
        bundle?.also {
            measurementUuid = it.getString("measurement_uuid") ?: ""
            val resultJson = it.getString("current_result") ?: ""
            if (resultJson.isNotBlank()) {
                measurementResult = DefaultJson.decodeFromString(resultJson)
            }
        }

        configureInputCounter()
        displayResultFeedbackRegions(measurementResult)
    }

    /***
     * Informs the user how many characters are left in the feedback comment
     */
    @SuppressLint("SetTextI18n")
    private fun configureInputCounter() {
        findViewById<TextView>(R.id.txtResultFeedback_token).text =
            "${getString(R.string.txt_token)} $measurementUuid"
        val tvInputCounter = findViewById<TextView>(R.id.tvResultFeedback_inputCounter)

        findViewById<EditText>(R.id.etResultFeedback_userInput).addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val remaining = 500 - s.length
                val c = "$remaining/500"
                tvInputCounter.text = c
            }
        })
    }

    /***
     * Displays each region to enable the user Feedback on the measured values
     */
    private fun displayResultFeedbackRegions(measurementResult: TreadDepthResult) {
        editTextsList.clear()
        val llMeasurementResultFeedbackRegions =
            findViewById<LinearLayout>(R.id.llMeasurementResultFeedbackRegions)
        // Divide the layout by the total of regions and spaces around them.
        llMeasurementResultFeedbackRegions.weightSum =
            (measurementResult.regions.size.toFloat() * 2)

        val useImperial = PreferencesUtils.shouldUseImperialSystem(this)

        // Display the regions dynamically, from left to right.
        for (i in 0 until measurementResult.regions.size) {
            val regionResultFeedbackFragment =
                layoutInflater.inflate(R.layout.fragment_region_result_feedback, null, false)

            val etRegionResultFeedback =
                regionResultFeedbackFragment.findViewById<EditText>(R.id.etRegionResultFeedback)
            if (useImperial) {
                etRegionResultFeedback.hint = "in"
            } else {
                etRegionResultFeedback.hint = "mm"

            }

            // define the ID of the EditText,
            // this is used by the previous EditText to focus on it when "Next" is pressed
            etRegionResultFeedback.id = i

            editTextsList.add(etRegionResultFeedback)
            llMeasurementResultFeedbackRegions.addView(regionResultFeedbackFragment)
        }
        // define where the focus should go after pressing "Next" on the keyboard
        // in this case, the next region should be focused
        for (i in 0 until editTextsList.size - 1) {
            editTextsList[i].nextFocusDownId = editTextsList[i + 1].id
        }
    }

    fun onClickedBtnCancel(view: View) {
        finish()
    }

    fun onClickedBtnSubmit(view: View) {
        val useImperial = PreferencesUtils.shouldUseImperialSystem(this)
        val commentText =
            (findViewById<EditText>(R.id.etResultFeedback_userInput)?.text ?: "").toString()
        val editedResult = editTextsList.map {
            val idx = editTextsList.indexOf(it)
            val region = measurementResult.regions[idx]

            if (useImperial) {
                TreadResultRegion.initInch(
                    it.text.isNotEmpty(),
                    region.confidence,
                    it.text.toString().toDoubleOrNull() ?: 0.0
                )
            } else {
                TreadResultRegion.initMm(
                    it.text.isNotEmpty(),
                    region.confidence,
                    it.text.toString().toDoubleOrNull() ?: 0.0
                )
            }
        }

        val jsonParser = Json(DefaultJson) { prettyPrint = true }
        AnylineTireTreadSdk.sendTreadDepthResultFeedback(measurementUuid, editedResult,
            onSendTreadDepthResultSucceed = {
                Log.i(
                    "SHOWCASE", "Send result feedback success. Your result =\n" +
                            jsonParser.encodeToString(editedResult)
                )

                Toast.makeText(
                    this,
                    resources.getString(R.string.txt_feedback_saved),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSendTreadDepthResultFailed = { _, e ->
                Log.e("SHOWCASE", "Send result feedback failed.", e)
                Toast.makeText(
                    this,
                    resources.getString(R.string.txt_feedback_failed),
                    Toast.LENGTH_SHORT
                ).show()
            })

        if (commentText.isNotBlank()) {
            AnylineTireTreadSdk.sendCommentFeedback(measurementUuid, commentText,
                onSendCommentSucceed = {
                    Log.i("SHOWCASE", "Send comment success. Your comment =\n$commentText")

                    Toast.makeText(
                        this,
                        resources.getString(R.string.txt_comment_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSendCommentFailed = { _, e ->
                    Log.e("SHOWCASE", "Send comment failed.", e)
                    Toast.makeText(
                        this,
                        resources.getString(R.string.txt_comment_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                })
        }
        Log.d("SHOWCASE", "UUID: $measurementUuid")

        finish()
    }
}