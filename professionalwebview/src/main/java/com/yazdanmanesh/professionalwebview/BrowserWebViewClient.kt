package com.yazdanmanesh.professionalwebview

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.net.http.SslError.SSL_UNTRUSTED
import android.os.Build
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread

class BrowserWebViewClient(
    private val specialUrlDetector: SpecialUrlDetector,
) : WebViewClient() {

    var webViewClientListener: WebViewClientListener? = null
    private var lastPageStarted: String? = null
    private var errorCode: Int = -1

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url

        return shouldOverride(view, url, request.isForMainFrame)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun shouldOverrideUrlLoading(
        view: WebView,
        urlString: String
    ): Boolean {
        val url = Uri.parse(urlString)
        return shouldOverride(view, url, isForMainFrame = true)
    }

    private fun shouldOverride(
        webView: WebView,
        url: Uri,
        isForMainFrame: Boolean
    ): Boolean {
        try {
            return when (val urlType = specialUrlDetector.determineType(url)) {
                is SpecialUrlDetector.UrlType.Email -> {
                    true
                }

                is SpecialUrlDetector.UrlType.Telephone -> {
                    webViewClientListener?.handleTelephone(url.toString())
                    true
                }

                is SpecialUrlDetector.UrlType.Sms -> {
                    true
                }

                is SpecialUrlDetector.UrlType.AppLink -> {
                    webViewClientListener?.let {}
                    false
                }

                is SpecialUrlDetector.UrlType.NonHttpAppLink -> {
                    webViewClientListener?.let { listener ->
                        return listener.handleNonHttpAppLink(urlType)
                    }
                    true
                }

                is SpecialUrlDetector.UrlType.Unknown -> {
                    webView.originalUrl?.let {
                        webView.loadUrl(it)
                    }
                    false
                }

                is SpecialUrlDetector.UrlType.SearchQuery -> false
                is SpecialUrlDetector.UrlType.Web -> {
                    false
                }

                is SpecialUrlDetector.UrlType.ExtractedTrackingLink -> {
                    if (isForMainFrame) {
                        webView.loadUrl(urlType.extractedUrl)
                        return true
                    }
                    false
                }

                else -> {
                    false
                }
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    @UiThread
    override fun onPageStarted(
        webView: WebView,
        url: String?,
        favicon: Bitmap?
    ) {
        try {
            lastPageStarted = url
            webViewClientListener?.onPageStarted(webView, url, favicon)
        } catch (e: Throwable) {
            throw e
        }
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        this.errorCode = errorCode
        webViewClientListener?.onReceivedError(view, errorCode, description, failingUrl)
    }

    @WorkerThread
    override fun shouldInterceptRequest(
        webView: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return super.shouldInterceptRequest(webView, request)
    }


    @UiThread
    override fun onPageFinished(
        webView: WebView,
        url: String?
    ) {
        CookieManager.getInstance().flush();
        try {
            webViewClientListener?.onPageFinished(webView, errorCode, url)
            errorCode = -1
        } catch (e: Throwable) {
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        return true
    }

    @UiThread
    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        try {
            if (handler != null) {
                requestAuthentication(view, handler, host, realm)
            } else {
                super.onReceivedHttpAuthRequest(view, handler, host, realm)
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler,
        error: SslError
    ) {
        when (error.primaryError) {
            SSL_UNTRUSTED -> {
            }
        }
    }

    private fun requestAuthentication(
        view: WebView?,
        handler: HttpAuthHandler,
        host: String?,
        realm: String?
    ) {
        webViewClientListener?.let {
        }
    }
}
