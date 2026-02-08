package com.yazdanmanesh.professionalwebview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Message
import android.provider.Settings
import android.util.AttributeSet
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference
import java.util.*

class ProfessionalWebView : WebView {

    private var activityRef: WeakReference<Activity?>? = null
    private val permittedHostnamesList: MutableList<String> = LinkedList()

    private var fileUploadCallback: ValueCallback<Array<Uri?>?>? = null
    private var languageIso3Cache: String? = null
    private var requestCodeFilePicker = REQUEST_CODE_FILE_PICKER
    private var customWebChromeClient: WebChromeClient? = null
    private var uploadableFileTypes = "*/*"
    private val httpHeaders: MutableMap<String, String> = HashMap()
    private var permissionRequest: PermissionRequest? = null
    private var geolocationCallback: GeolocationPermissions.Callback? = null
    private var geolocationOrigin: String? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context)
    }

    override fun setWebChromeClient(client: WebChromeClient?) {
        customWebChromeClient = client
    }

    fun setGeolocationEnabled(enabled: Boolean) {
        if (enabled) {
            settings.javaScriptEnabled = true
            settings.setGeolocationEnabled(true)
        }
    }

    fun setUploadableFileTypes(mimeType: String) {
        uploadableFileTypes = mimeType
    }

    @JvmOverloads
    fun loadHtml(
        html: String?,
        baseUrl: String? = null,
        historyUrl: String? = null,
        encoding: String? = "utf-8"
    ) {
        html?.let {
            loadDataWithBaseURL(baseUrl, it, "text/html", encoding, historyUrl)
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTimers()
    }

    override fun onPause() {
        pauseTimers()
        super.onPause()
    }

    fun onDestroy() {
        (parent as? ViewGroup)?.removeView(this)
        removeAllViews()
        destroy()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == requestCodeFilePicker) {
            if (resultCode == Activity.RESULT_OK) {
                intent?.let {
                    if (fileUploadCallback != null) {
                        val dataUris = try {
                            if (it.dataString != null) {
                                arrayOf(Uri.parse(it.dataString))
                            } else {
                                if (it.clipData != null) {
                                    val numSelectedFiles = it.clipData!!.itemCount
                                    Array<Uri?>(numSelectedFiles) { i ->
                                        it.clipData!!.getItemAt(i).uri
                                    }
                                } else null
                            }
                        } catch (ignored: Exception) {
                            null
                        }
                        fileUploadCallback?.onReceiveValue(dataUris)
                        fileUploadCallback = null
                    }
                }
            } else {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = null
            }
        }
    }

    fun addHttpHeader(name: String, value: String) {
        httpHeaders[name] = value
    }

    fun removeHttpHeader(name: String) {
        httpHeaders.remove(name)
    }

    fun addPermittedHostname(hostname: String) {
        permittedHostnamesList.add(hostname)
    }

    fun addPermittedHostnames(collection: Collection<String>?) {
        collection?.let { permittedHostnamesList.addAll(it) }
    }

    val permittedHostnames: List<String>
        get() = permittedHostnamesList

    fun removePermittedHostname(hostname: String) {
        permittedHostnamesList.remove(hostname)
    }

    fun clearPermittedHostnames() {
        permittedHostnamesList.clear()
    }

    fun onBackPressed(): Boolean {
        return if (canGoBack()) {
            goBack()
            false
        } else {
            true
        }
    }

    fun setCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptCookie(enabled)
    }

    fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled)
    }

    fun setMixedContentAllowed(allowed: Boolean) {
        setMixedContentAllowed(settings, allowed)
    }

    private fun setMixedContentAllowed(webSettings: WebSettings, allowed: Boolean) {
        webSettings.mixedContentMode =
            if (allowed) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
    }

    fun setDesktopMode(enabled: Boolean) {
        val webSettings = settings
        val newUserAgent = if (enabled) {
            webSettings.userAgentString.replace("Mobile", "eliboM").replace("Android", "diordnA")
        } else {
            webSettings.userAgentString.replace("eliboM", "Mobile").replace("diordnA", "Android")
        }
        webSettings.userAgentString = newUserAgent
        with(webSettings) {
            useWideViewPort = enabled
            loadWithOverviewMode = enabled
            setSupportZoom(enabled)
            builtInZoomControls = enabled
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init(context: Context) {
        if (isInEditMode) {
            return
        }
        if (context is Activity) {
            activityRef = WeakReference(context)
        }
        languageIso3Cache = languageIso3
        isFocusable = true
        isFocusableInTouchMode = true
        isSaveEnabled = true
        val webSettings = settings
        with(webSettings) {
            allowFileAccess = false
            builtInZoomControls = false
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            allowContentAccess = false
        }
        setThirdPartyCookiesEnabled(true)

        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        setWebContentsDebuggingEnabled(isDebuggable)

        super.setWebChromeClient(object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri?>?>?,
                fileChooserParams: FileChooserParams
            ): Boolean {
                val allowMultiple =
                    fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                openFileInput(filePathCallback, allowMultiple)
                return true
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                customWebChromeClient?.onProgressChanged(view, newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                customWebChromeClient?.onReceivedTitle(view, title)
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                customWebChromeClient?.onReceivedIcon(view, icon)
            }

            override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
                customWebChromeClient?.onReceivedTouchIconUrl(view, url, precomposed)
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customWebChromeClient?.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {
                customWebChromeClient?.onHideCustomView() ?: super.onHideCustomView()
            }

            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                return customWebChromeClient?.onCreateWindow(
                    view, isDialog, isUserGesture, resultMsg
                ) ?: super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onRequestFocus(view: WebView) {
                customWebChromeClient?.onRequestFocus(view) ?: super.onRequestFocus(view)
            }

            override fun onCloseWindow(view: WebView) {
                customWebChromeClient?.onCloseWindow(view) ?: super.onCloseWindow(view)
            }

            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                return customWebChromeClient?.onJsAlert(view, url, message, result)
                    ?: super.onJsAlert(view, url, message, result)
            }

            override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                return customWebChromeClient?.onJsConfirm(view, url, message, result)
                    ?: super.onJsConfirm(view, url, message, result)
            }

            override fun onJsPrompt(
                view: WebView,
                url: String,
                message: String,
                defaultValue: String,
                result: JsPromptResult
            ): Boolean {
                return customWebChromeClient?.onJsPrompt(view, url, message, defaultValue, result)
                    ?: super.onJsPrompt(view, url, message, defaultValue, result)
            }

            override fun onJsBeforeUnload(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                return customWebChromeClient?.onJsBeforeUnload(view, url, message, result)
                    ?: super.onJsBeforeUnload(view, url, message, result)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String, callback: GeolocationPermissions.Callback
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    geolocationCallback = callback
                    geolocationOrigin = origin
                    activityRef?.get()?.let {
                        ActivityCompat.requestPermissions(
                            it,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_REQUEST_CODE,
                        )
                    }
                } else {
                    callback.invoke(origin, true, false)
                }
            }

            override fun onGeolocationPermissionsHidePrompt() {
                customWebChromeClient?.onGeolocationPermissionsHidePrompt()
                    ?: super.onGeolocationPermissionsHidePrompt()
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                ) {
                    permissionRequest = request
                    activityRef?.get()?.let {
                        ActivityCompat.requestPermissions(
                            it,
                            arrayOf(Manifest.permission.CAMERA),
                            CAMERA_REQUEST_CODE,
                        )
                    }
                } else {
                    request.grant(request.resources)
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                customWebChromeClient?.onPermissionRequestCanceled(request)
                    ?: super.onPermissionRequestCanceled(request)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return customWebChromeClient?.onConsoleMessage(consoleMessage)
                    ?: super.onConsoleMessage(consoleMessage)
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return customWebChromeClient?.defaultVideoPoster
                    ?: super.getDefaultVideoPoster()
            }

            override fun getVideoLoadingProgressView(): View? {
                return customWebChromeClient?.videoLoadingProgressView
                    ?: super.getVideoLoadingProgressView()
            }

            override fun getVisitedHistory(callback: ValueCallback<Array<String>>) {
                customWebChromeClient?.getVisitedHistory(callback)
                    ?: super.getVisitedHistory(callback)
            }
        })
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        val mergedHeaders = additionalHttpHeaders.toMutableMap()
        if (httpHeaders.isNotEmpty()) {
            mergedHeaders.putAll(httpHeaders)
        }
        super.loadUrl(url, mergedHeaders)
    }

    override fun loadUrl(url: String) {
        if (httpHeaders.isNotEmpty()) {
            super.loadUrl(url, httpHeaders)
        } else {
            super.loadUrl(url)
        }
    }

    fun loadUrl(url: String, preventCaching: Boolean) {
        val finalUrl = if (preventCaching) makeUrlUnique(url) else url
        loadUrl(finalUrl)
    }

    fun loadUrl(
        url: String, preventCaching: Boolean, additionalHttpHeaders: MutableMap<String, String>
    ) {
        val finalUrl = if (preventCaching) makeUrlUnique(url) else url
        val headers = additionalHttpHeaders.ifEmpty { httpHeaders }
        loadUrl(finalUrl, headers)
    }

    fun isPermittedUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            return false
        }

        try {
            val parsedUrl = Uri.parse(url)
            val actualHost = parsedUrl.host ?: return false

            if (!actualHost.matches(Regex("^[a-zA-Z0-9._!~*')(;:&=+$,%\\[\\]-]*$"))) {
                return false
            }

            val actualUserInformation = parsedUrl.userInfo
            if (actualUserInformation != null && !actualUserInformation.matches(Regex("^[a-zA-Z0-9._!~*')(;:&=+$,%-]*$"))) {
                return false
            }

            for (expectedHost in permittedHostnamesList) {
                if (actualHost == expectedHost || actualHost.endsWith(".$expectedHost")) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    private val fileUploadPromptLabel: String
        get() {
            try {
                return when (languageIso3Cache) {
                    "zho" -> decodeBase64("6YCJ5oup5LiA5Liq5paH5Lu2")
                    "spa" -> decodeBase64("RWxpamEgdW4gYXJjaGl2bw==")
                    "hin" -> decodeBase64("4KSP4KSVIOCkq+CkvOCkvuCkh+CksiDgpJrgpYHgpKjgpYfgpII=")
                    "ben" -> decodeBase64("4KaP4KaV4Kaf4Ka/IOCmq+CmvuCmh+CmsiDgpqjgpr/gprDgp43gpqzgpr7gpprgpqg=")
                    "ara" -> decodeBase64("2KfYrtiq2YrYp9ixINmF2YTZgSDZiNin2K3Yrw==")
                    "por" -> decodeBase64("RXNjb2xoYSB1bSBhcnF1aXZv")
                    "rus" -> decodeBase64("0JLRi9Cx0LXRgNC40YLQtSDQvtC00LjQvSDRhNCw0LnQuw==")
                    "jpn" -> decodeBase64("MeODleOCoeOCpOODq+OCkumBuOaKnuOBl+OBpuOBj+OBoOOBleOBhA==")
                    "pan" -> decodeBase64("4KiH4Kmx4KiVIOCoq+CovuCoh+CosiDgqJrgqYHgqKPgqYs=")
                    "deu" -> decodeBase64("V8OkaGxlIGVpbmUgRGF0ZWk=")
                    "jav" -> decodeBase64("UGlsaWggc2lqaSBiZXJrYXM=")
                    "msa" -> decodeBase64("UGlsaWggc2F0dSBmYWls")
                    "tel" -> decodeBase64("4LCS4LCVIOCwq+CxhuCxluCwsuCxjeCwqOCxgSDgsI7gsILgsJrgsYHgsJXgsYvgsILgsKHgsL8=")
                    "vie" -> decodeBase64("Q2jhu41uIG3hu5l0IHThuq1wIHRpbg==")
                    "kor" -> decodeBase64("7ZWY64KY7J2YIO2MjOydvOydhCDshKDtg50=")
                    "fra" -> decodeBase64("Q2hvaXNpc3NleiB1biBmaWNoaWVy")
                    "mar" -> decodeBase64("4KSr4KS+4KSH4KSyIOCkqOCkv+CkteCkoeCkvg==")
                    "tam" -> decodeBase64("4K6S4K6w4K+BIOCuleCvh+CuvuCuquCvjeCuquCviCDgrqTgr4fgrrDgr43grrXgr4E=")
                    "urd" -> decodeBase64("2KfbjNqpINmB2KfYptmEINmF24zauiDYs9uSINin2YbYqtiu2KfYqCDaqdix24zaug==")
                    "fas" -> decodeBase64("2LHYpyDYp9mG2KrYrtin2Kgg2qnZhtuM2K8g24zaqSDZgdin24zZhA==")
                    "tur" -> decodeBase64("QmlyIGRvc3lhIHNlw6dpbg==")
                    "ita" -> decodeBase64("U2NlZ2xpIHVuIGZpbGU=")
                    "tha" -> decodeBase64("4LmA4Lil4Li34Lit4LiB4LmE4Lif4Lil4LmM4Lir4LiZ4Li24LmI4LiH")
                    "guj" -> decodeBase64("4KqP4KqVIOCqq+CqvuCqh+CqsuCqqOCrhyDgqqrgqrjgqoLgqqY=")
                    else -> "Choose a file"
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
            return "Choose a file"
        }

    private fun decodeBase64(encoded: String): String {
        return String(
            Base64.decode(encoded, Base64.DEFAULT),
            Charsets.UTF_8
        )
    }

    private fun openFileInput(
        callback: ValueCallback<Array<Uri?>?>?,
        allowMultiple: Boolean
    ) {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = callback

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = uploadableFileTypes
            if (allowMultiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        activityRef?.get()?.startActivityForResult(
            Intent.createChooser(intent, fileUploadPromptLabel),
            requestCodeFilePicker
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (granted) {
                permissionRequest?.grant(permissionRequest?.resources)
            } else {
                permissionRequest?.deny()
            }
            permissionRequest = null
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (granted) {
                geolocationCallback?.invoke(geolocationOrigin, true, false)
            } else {
                geolocationCallback?.invoke(geolocationOrigin, false, false)
            }
            geolocationCallback = null
            geolocationOrigin = null
        }
    }

    companion object {
        private const val REQUEST_CODE_FILE_PICKER = 51426
        const val CAMERA_REQUEST_CODE = 113
        const val LOCATION_REQUEST_CODE = 115
        private const val LANGUAGE_DEFAULT_ISO3 = "eng"

        private fun makeUrlUnique(url: String): String {
            val unique = StringBuilder()
            unique.append(url)
            if (url.contains("?")) {
                unique.append('&')
            } else {
                if (url.lastIndexOf('/') <= 7) {
                    unique.append('/')
                }
                unique.append('?')
            }
            unique.append(System.currentTimeMillis())
            unique.append('=')
            unique.append(1)
            return unique.toString()
        }

        private val languageIso3: String
            get() = try {
                Locale.getDefault().isO3Language.lowercase(Locale.US)
            } catch (e: MissingResourceException) {
                LANGUAGE_DEFAULT_ISO3
            }

        fun handleDownload(context: Context, fromUrl: String?, toFilename: String?): Boolean {
            val request = DownloadManager.Request(Uri.parse(fromUrl))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return try {
                dm.enqueue(request)
                true
            } catch (e: IllegalArgumentException) {
                openAppSettings(context, PACKAGE_NAME_DOWNLOAD_MANAGER)
                false
            }
        }

        private const val PACKAGE_NAME_DOWNLOAD_MANAGER = "com.android.providers.downloads"

        private fun openAppSettings(context: Context, packageName: String): Boolean {
            return try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
