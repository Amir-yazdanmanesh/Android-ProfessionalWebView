package com.yazdanmanesh.sample

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yazdanmanesh.professionalwebview.BrowserWebViewClient
import com.yazdanmanesh.professionalwebview.SpecialUrlDetector
import com.yazdanmanesh.professionalwebview.SpecialUrlDetectorImpl
import com.yazdanmanesh.professionalwebview.WebViewClientListener
import com.yazdanmanesh.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WebViewClientListener {

    private lateinit var binding: ActivityMainBinding

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        binding.browserWebView.onFilePickerResult(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.browserWebView.setFilePickerLauncher { mimeType, _ ->
            filePickerLauncher.launch(arrayOf(mimeType))
        }

        val specialUrlDetectorImpl = SpecialUrlDetectorImpl(this)
        val browserWebViewClient = BrowserWebViewClient(specialUrlDetectorImpl)
        browserWebViewClient.webViewClientListener = this
        binding.browserWebView.webViewClient = browserWebViewClient
        navigate("https://example.com/")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.browserWebView.onBackPressed()) {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.browserWebView.onResume()
    }

    override fun onPause() {
        binding.browserWebView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.browserWebView.onDestroy()
        super.onDestroy()
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
        binding.webviewLoading.visibility = View.VISIBLE
    }

    override fun onReceivedError(
        view: WebView, errorCode: Int, description: String?, failingUrl: String?
    ) {
        Log.d(TAG, "onPageError(errorCode=$errorCode, description=$description, failingUrl=$failingUrl)")
    }

    override fun onPageFinished(webView: WebView, errorCode: Int, url: String?) {
        binding.webviewLoading.visibility = View.GONE
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
                fallbackUrl != null -> navigate(fallbackUrl)
                else -> Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigate(url: String) {
        binding.browserWebView.loadUrl(url)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        binding.browserWebView.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
