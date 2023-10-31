package io.anyline.tiretread.demo.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.anyline.tiretread.demo.common.PreferencesUtils
import io.anyline.tiretread.demo.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpWebView()
        binding.webView.loadDataWithBaseURL(
            null, getHtml(), "text/html", "utf8", null
        )
    }

    override fun onResume() {
        super.onResume()
        PreferencesUtils.onTutorialShown(this)
    }

    private fun getHtml(): String {
        return """
                    <html>
                        <head>
                            <meta 
                                name="viewport" 
                                content="width=device-width, initial-scale=1.0 maximum-scale=1.0 user-scalable=no" 
                                charset="utf-8" 
                            />
                        </head>
                        
                        <body>
                            <iframe 
                                src="$VIDEO_URL" 
                                style="position:absolute;top:0;left:0;width:100%;height:100%;"
                                frameborder="0" 
                                allow="autoplay"
                                allowfullscreen
                            />
                            <script src="https://player.vimeo.com/api/player.js"></script>
                        </body>
                    </html>
                """.trimIndent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                //needs to be here otherwise the links are not opened or redirected to an external browser
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    return super.shouldOverrideUrlLoading(view, request)
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                }
            }

            webChromeClient = object : WebChromeClient() {

                private var customView: View? = null
                private var customViewCallback: CustomViewCallback? = null
                private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                private var originalSystemVisibility = 0

                override fun onJsConfirm(
                    view: WebView?, url: String?, message: String?, result: JsResult?
                ): Boolean {
                    AlertDialog.Builder(this@TutorialActivity).also { dialogBuilder ->
                        dialogBuilder.setMessage(message)
                        dialogBuilder.setPositiveButton("OK") { _, _ -> result?.confirm() }
                        dialogBuilder.setNegativeButton("Cancel") { _, _ -> result?.cancel() }
                        dialogBuilder.create()
                        dialogBuilder.show()
                    }
                    return true
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    title?.let {
                        setTitle(it)
                    }
                    super.onReceivedTitle(view, title)
                }

                override fun onHideCustomView() {
                    // on below line removing our custom view and setting it to null.
                    (window.decorView as FrameLayout).removeView(customView)
                    customView = null

                    // on below line setting system ui visibility to original one and setting orientation for it.
                    window.decorView.systemUiVisibility = this.originalSystemVisibility
                    requestedOrientation = this.originalOrientation

                    // on below line setting custom view call back to null.
                    this.customViewCallback?.onCustomViewHidden()
                    this.customViewCallback = null
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (this.customView != null) {
                        onHideCustomView()
                        return
                    }
                    // on below line initializing all variables.
                    this.customView = view
                    originalSystemVisibility = window.decorView.systemUiVisibility
                    originalOrientation = requestedOrientation
                    this.customViewCallback = callback
                    (window.decorView as FrameLayout).addView(
                        this.customView, FrameLayout.LayoutParams(-1, -1)
                    )
                    window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
                }
            }

            settings.apply {
                javaScriptEnabled = true
            }
        }
    }

    companion object {
        private const val VIDEO_URL =
            "https://player.vimeo.com/video/774805672?background=0&autoplay=1&muted=0&playsinline=1"
    }

    fun onClickedButtonCancel(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}