package com.yazdanmanesh.sample

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yazdanmanesh.professionalwebview.BrowserWebViewClient
import com.yazdanmanesh.professionalwebview.ProfessionalWebView
import com.yazdanmanesh.professionalwebview.SpecialUrlDetector
import com.yazdanmanesh.professionalwebview.SpecialUrlDetectorImpl
import com.yazdanmanesh.professionalwebview.WebViewClientListener
import com.yazdanmanesh.professionalwebview.rememberProfessionalWebViewState

class MainActivity : AppCompatActivity(), WebViewClientListener {

    private var isLoading by mutableStateOf(false)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        webViewState?.webView?.onFilePickerResult(uris)
    }

    private var webViewState: com.yazdanmanesh.professionalwebview.ProfessionalWebViewState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val state = rememberProfessionalWebViewState()
                webViewState = state

                Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    ProfessionalWebView(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        onCreated = { webView ->
                            webView.setFilePickerLauncher { mimeType, _ ->
                                filePickerLauncher.launch(arrayOf(mimeType))
                            }

                            val specialUrlDetector = SpecialUrlDetectorImpl(this@MainActivity)
                            val browserWebViewClient = BrowserWebViewClient(specialUrlDetector)
                            browserWebViewClient.webViewClientListener = this@MainActivity
                            webView.webViewClient = browserWebViewClient
                            webView.loadUrl("https://www.google.com/")
                        },
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                BackHandler {
                    val finished = state.webView?.onBackPressed() ?: true
                    if (finished) {
                        finish()
                    }
                }
            }
        }
    }

    override fun handleTelephone(tel: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$tel")
        startActivity(intent)
    }

    override fun handleEmail(emailAddress: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse(emailAddress)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    override fun handleSms(smsUri: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$smsUri")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    override fun handleAppLink(appLink: SpecialUrlDetector.UrlType.AppLink): Boolean {
        appLink.appIntent?.let { intent ->
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                return true
            }
        }
        return false
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
        isLoading = true
    }

    override fun onReceivedError(
        view: WebView, errorCode: Int, description: String?, failingUrl: String?
    ) {
        Log.d(TAG, "onPageError(errorCode=$errorCode, description=$description, failingUrl=$failingUrl)")
    }

    override fun onPageFinished(webView: WebView, errorCode: Int, url: String?) {
        isLoading = false
    }

    private fun openExternalDialog(
        intent: Intent,
        title: String?,
        fallbackUrl: String? = null,
    ) {
        val activities = packageManager.queryIntentActivities(intent, 0)

        if (activities.isNotEmpty()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            when {
                fallbackUrl != null -> webViewState?.webView?.loadUrl(fallbackUrl)
                else -> Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        webViewState?.webView?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
