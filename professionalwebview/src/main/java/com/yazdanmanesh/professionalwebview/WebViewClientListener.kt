package com.yazdanmanesh.professionalwebview

import android.graphics.Bitmap
import android.webkit.WebView
import com.yazdanmanesh.professionalwebview.SpecialUrlDetector


interface WebViewClientListener {
    fun handleTelephone(tel: String)
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
}
