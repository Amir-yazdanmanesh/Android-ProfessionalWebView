/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yazdanmanesh.professionalwebview

import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread

class BrowserWebViewClient(
    private val specialUrlDetector: SpecialUrlDetector,
) : WebViewClient() {

    var webViewClientListener: WebViewClientListener? = null
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
        return when (val urlType = specialUrlDetector.determineType(url)) {
            is SpecialUrlDetector.UrlType.Email -> {
                webViewClientListener?.handleEmail(urlType.emailAddress)
                true
            }

            is SpecialUrlDetector.UrlType.Telephone -> {
                webViewClientListener?.handleTelephone(urlType.telephoneNumber)
                true
            }

            is SpecialUrlDetector.UrlType.Sms -> {
                webViewClientListener?.handleSms(urlType.telephoneNumber)
                true
            }

            is SpecialUrlDetector.UrlType.AppLink -> {
                webViewClientListener?.handleAppLink(urlType) ?: false
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
            is SpecialUrlDetector.UrlType.Web -> false

            is SpecialUrlDetector.UrlType.ExtractedTrackingLink -> {
                if (isForMainFrame) {
                    webView.loadUrl(urlType.extractedUrl)
                    return true
                }
                false
            }
        }
    }

    @UiThread
    override fun onPageStarted(
        webView: WebView,
        url: String?,
        favicon: android.graphics.Bitmap?
    ) {
        webViewClientListener?.onPageStarted(webView, url, favicon)
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
        CookieManager.getInstance().flush()
        webViewClientListener?.onPageFinished(webView, errorCode, url)
        errorCode = -1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        return webViewClientListener?.onRenderProcessGone() ?: true
    }

    @UiThread
    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        if (handler != null) {
            webViewClientListener?.requiresAuthentication(host, realm, handler)
                ?: handler.cancel()
        } else {
            super.onReceivedHttpAuthRequest(view, handler, host, realm)
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler,
        error: SslError
    ) {
        webViewClientListener?.onSslErrorReceived(handler, error)
            ?: handler.cancel()
    }
}
