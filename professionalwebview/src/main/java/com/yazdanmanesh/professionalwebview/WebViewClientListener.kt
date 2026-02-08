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

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebView

interface WebViewClientListener {
    fun handleTelephone(tel: String)
    fun handleEmail(emailAddress: String) {}
    fun handleSms(smsUri: String) {}
    fun handleAppLink(appLink: SpecialUrlDetector.UrlType.AppLink): Boolean = false
    fun handleNonHttpAppLink(nonHttpAppLink: SpecialUrlDetector.UrlType.NonHttpAppLink): Boolean

    fun onPageStarted(
        webView: WebView,
        url: String?,
        favicon: Bitmap?
    )

    fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    )

    fun onPageFinished(
        webView: WebView, errorCode: Int,
        url: String?
    )

    fun onSslErrorReceived(handler: SslErrorHandler, error: SslError) {
        handler.cancel()
    }

    fun requiresAuthentication(host: String?, realm: String?, handler: HttpAuthHandler) {
        handler.cancel()
    }

    fun onRenderProcessGone(): Boolean = true
}
