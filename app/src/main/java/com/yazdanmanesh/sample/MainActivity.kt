package com.yazdanmanesh.sample

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yazdanmanesh.professionalwebview.BrowserWebViewClient
import com.yazdanmanesh.professionalwebview.ProfessionalWebView
import com.yazdanmanesh.professionalwebview.SpecialUrlDetector
import com.yazdanmanesh.professionalwebview.SpecialUrlDetectorImpl
import com.yazdanmanesh.professionalwebview.WebViewClientListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), WebViewClientListener {
    lateinit var webView: ProfessionalWebView
    lateinit var loading: ProgressBar
    private var loadingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val specialUrlDetectorImpl = SpecialUrlDetectorImpl(this)
        webView = findViewById(R.id.browserWebView)
        loading = findViewById(R.id.webview_loading)

        val browserWebViewClient = BrowserWebViewClient(specialUrlDetectorImpl)
        browserWebViewClient.webViewClientListener = this
        webView.let {
            it.webViewClient = browserWebViewClient
            navigate("https://example.com/")

        }
    }

    override fun handleTelephone(tel: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse(tel)
        startActivity(intent)
    }

    override fun handleNonHttpAppLink(nonHttpAppLink: SpecialUrlDetector.UrlType.NonHttpAppLink): Boolean {
        openExternalDialog(
            intent = nonHttpAppLink.intent,
            title = nonHttpAppLink.title,
            fallbackUrl = nonHttpAppLink.fallbackUrl,
        )
        return true
    }

    override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
        loading.visibility = View.VISIBLE
    }

    /**
     * Handles errors encountered during WebView loading.
     *
     * This method is called when an error occurs while loading a web page in the WebView.
     * It reports the error to Sentry for debugging and analysis purposes.
     * Additionally, if the application is running in debug mode, it displays a toast message
     * with details of the error for immediate visibility to developers.
     *
     * @param view The WebView that encountered the error.
     * @param errorCode The error code indicating the type of error.
     * @param description A description of the error.
     * @param failingUrl The URL that failed to load.
     */
    override fun onReceivedError(
        view: WebView, errorCode: Int, description: String?, failingUrl: String?
    ) {
        Log.d(
            "Tag",
            "onPageError(errorCode = $errorCode,  description = $description,  failingUrl = $failingUrl)"
        )
    }


    override fun onPageFinished(webView: WebView, errorCode: Int, url: String?) {
        loadingJob?.cancel()
        loadingJob = lifecycleScope.launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                loading.visibility = View.GONE
            }
        }
    }

    /**
     * Opens an external app or URL based on the provided intent.
     *
     * @param intent The intent to open the external app or URL.
     * @param title The title associated with the action.
     * @param fallbackUrl Optional fallback URL if the intent cannot be resolved.
     */
    private fun openExternalDialog(
        intent: Intent,
        title: String?,
        fallbackUrl: String? = null,
    ) {
        let {
            val pm = packageManager
            val activities = pm.queryIntentActivities(intent, 0)

            if (activities.isEmpty()) {
                when {
                    fallbackUrl != null -> {
                        val appLinkData = Uri.parse(fallbackUrl).buildUpon()
                        appLinkData.appendQueryParameter("persion_title", title)
                        appLinkData.build()
                        navigate(appLinkData.toString())
                    }

                    else -> {
                        Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Navigates to the specified URL in the WebView.
     *
     * @param url The URL to navigate to.
     */
    private fun navigate(url: String) {
        webView.loadUrl(url)
    }
}