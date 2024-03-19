package com.yazdanmanesh.professionalwebview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
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
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.util.*

class ProfessionalWebView : WebView {

    private var mActivity: WeakReference<Activity?>? = null
    private val mPermittedHostnames: MutableList<String> = LinkedList()

    /**
     * File upload callback for platform versions prior to Android 5.0
     */
    private var mFileUploadCallbackFirst: ValueCallback<Uri?>? = null

    /**
     * File upload callback for Android 5.0+
     */
    private var mFileUploadCallbackSecond: ValueCallback<Array<Uri?>?>? = null
    private var mLastError: Long = 0
    private var mLanguageIso3: String? = null
    private var mRequestCodeFilePicker = REQUEST_CODE_FILE_PICKER
    private var mCustomWebChromeClient: WebChromeClient? = null
    private var mGeolocationEnabled = false
    private var mUploadableFileTypes = "*/*"
    private val mHttpHeaders: MutableMap<String, String> = HashMap()

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
        mCustomWebChromeClient = client
    }

    fun setGeolocationEnabled(enabled: Boolean) {
        if (enabled) {
            settings.javaScriptEnabled = true
            settings.setGeolocationEnabled(true)
        }
        mGeolocationEnabled = enabled
    }


    fun setUploadableFileTypes(mimeType: String) {
        mUploadableFileTypes = mimeType
    }
    /**
     * Loads and displays the provided HTML source text
     *
     * @param html       the HTML source text to load
     * @param baseUrl    the URL to use as the page's base URL
     * @param historyUrl the URL to use for the page's history entry
     * @param encoding   the encoding or charset of the HTML source text
     */
    /**
     * Loads and displays the provided HTML source text
     *
     * @param html       the HTML source text to load
     * @param baseUrl    the URL to use as the page's base URL
     * @param historyUrl the URL to use for the page's history entry
     */
    /**
     * Loads and displays the provided HTML source text
     *
     * @param html    the HTML source text to load
     * @param baseUrl the URL to use as the page's base URL
     */
    /**
     * Loads and displays the provided HTML source text
     *
     * @param html the HTML source text to load
     */
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
        // Remove this view from its parent
        (parent as? ViewGroup)?.removeView(this)

        // Remove all child views from this view
        removeAllViews()

        // Destroy this view
        destroy()
    }


    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == mRequestCodeFilePicker) {
            if (resultCode == Activity.RESULT_OK) {
                intent?.let {
                    if (mFileUploadCallbackFirst != null) {
                        mFileUploadCallbackFirst!!.onReceiveValue(it.data)
                        mFileUploadCallbackFirst = null
                    } else if (mFileUploadCallbackSecond != null) {
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
                        mFileUploadCallbackSecond!!.onReceiveValue(dataUris)
                        mFileUploadCallbackSecond = null
                    }
                }
            } else {
                mFileUploadCallbackFirst?.onReceiveValue(null)
                mFileUploadCallbackSecond?.onReceiveValue(null)
                mFileUploadCallbackFirst = null
                mFileUploadCallbackSecond = null
            }
        }
    }

    /**
     * Adds an additional HTTP header that will be sent along with every HTTP `GET` request
     *
     *
     * This does only affect the main requests, not the requests to included resources (e.g. images)
     *
     *
     * If you later want to delete an HTTP header that was previously added this way, call `removeHttpHeader()`
     *
     *
     * The `WebView` implementation may in some cases overwrite headers that you set or unset
     *
     * @param name  the name of the HTTP header to add
     * @param value the value of the HTTP header to send
     */
    fun addHttpHeader(name: String, value: String) {
        mHttpHeaders[name] = value
    }

    /**
     * Removes one of the HTTP headers that have previously been added via `addHttpHeader()`
     *
     *
     * If you want to unset a pre-defined header, set it to an empty string with `addHttpHeader()` instead
     *
     *
     * The `WebView` implementation may in some cases overwrite headers that you set or unset
     *
     * @param name the name of the HTTP header to remove
     */
    fun removeHttpHeader(name: String) {
        mHttpHeaders.remove(name)
    }

    fun addPermittedHostname(hostname: String) {
        mPermittedHostnames.add(hostname)
    }

    fun addPermittedHostnames(collection: Collection<String>?) {
        mPermittedHostnames.addAll(collection!!)
    }

    val permittedHostnames: List<String>
        get() = mPermittedHostnames

    fun removePermittedHostname(hostname: String) {
        mPermittedHostnames.remove(hostname)
    }

    fun clearPermittedHostnames() {
        mPermittedHostnames.clear()
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


    private fun init(context: Context) {
        // in IDE's preview mode
        if (isInEditMode) {
            return
        }
        if (context is Activity) {
            mActivity = WeakReference(context)
        }
        mLanguageIso3 = languageIso3
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
            setThirdPartyCookiesEnabled(true)
            setWebContentsDebuggingEnabled(true)
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            // Keeping these off is less critical but still a good idea, especially if your app is not
            // using file:// or content:// URLs.
            allowFileAccess = false
            allowContentAccess = false
        }


        super.setWebChromeClient(object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri?>?>?,
                fileChooserParams: FileChooserParams
            ): Boolean {
                val allowMultiple =
                    fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                openFileInput(null, filePathCallback, allowMultiple)
                return true
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                mCustomWebChromeClient?.onProgressChanged(view, newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                mCustomWebChromeClient?.onReceivedTitle(view, title)
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                mCustomWebChromeClient?.onReceivedIcon(view, icon)
            }

            override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
                mCustomWebChromeClient?.onReceivedTouchIconUrl(view, url, precomposed)
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                mCustomWebChromeClient?.onShowCustomView(view, callback)
            }


            override fun onHideCustomView() {
                mCustomWebChromeClient?.onHideCustomView() ?: super.onHideCustomView()
            }


            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                return mCustomWebChromeClient?.onCreateWindow(
                    view,
                    isDialog,
                    isUserGesture,
                    resultMsg
                )
                    ?: super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onRequestFocus(view: WebView) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onRequestFocus(view)
                } else {
                    super.onRequestFocus(view)
                }
            }

            override fun onCloseWindow(view: WebView) {
                mCustomWebChromeClient?.onRequestFocus(view) ?: super.onRequestFocus(view)
            }

            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                return mCustomWebChromeClient?.onJsAlert(view, url, message, result)
                    ?: super.onJsAlert(view, url, message, result)
            }


            override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                return mCustomWebChromeClient?.onJsConfirm(view, url, message, result)
                    ?: super.onJsConfirm(view, url, message, result)
            }


            override fun onJsPrompt(
                view: WebView,
                url: String,
                message: String,
                defaultValue: String,
                result: JsPromptResult
            ): Boolean {
                return mCustomWebChromeClient?.onJsPrompt(view, url, message, defaultValue, result)
                    ?: super.onJsPrompt(view, url, message, defaultValue, result)

            }

            override fun onJsBeforeUnload(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                return mCustomWebChromeClient?.onJsBeforeUnload(view, url, message, result)
                    ?: super.onJsBeforeUnload(view, url, message, result)

            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String, callback: GeolocationPermissions.Callback
            ) {
                if (mGeolocationEnabled) {
                    callback.invoke(origin, true, false)
                } else {
                    mCustomWebChromeClient?.onGeolocationPermissionsShowPrompt(origin, callback)
                        ?: super.onGeolocationPermissionsShowPrompt(origin, callback)
                }
            }

            override fun onGeolocationPermissionsHidePrompt() {
                mCustomWebChromeClient?.onGeolocationPermissionsHidePrompt()
                    ?: super.onGeolocationPermissionsHidePrompt()
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                mCustomWebChromeClient?.onPermissionRequest(request)
                    ?: super.onPermissionRequest(request)


            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                mCustomWebChromeClient?.onPermissionRequestCanceled(request)
                    ?: super.onPermissionRequestCanceled(request)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return mCustomWebChromeClient?.onConsoleMessage(consoleMessage)
                    ?: super.onConsoleMessage(consoleMessage)

            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return mCustomWebChromeClient?.defaultVideoPoster
                    ?: super.getDefaultVideoPoster()
            }

            override fun getVideoLoadingProgressView(): View? {
                return mCustomWebChromeClient?.videoLoadingProgressView
                    ?: super.getVideoLoadingProgressView()
            }

            override fun getVisitedHistory(callback: ValueCallback<Array<String>>) {
                mCustomWebChromeClient?.getVisitedHistory(callback)
                    ?: super.getVisitedHistory(callback)

            }
        })
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        val mergedHeaders = additionalHttpHeaders.toMutableMap()
        if (mHttpHeaders.isNotEmpty()) {
            mergedHeaders.putAll(mHttpHeaders)
        }
        super.loadUrl(url, mergedHeaders)
    }


    override fun loadUrl(url: String) {
        if (mHttpHeaders.isNotEmpty()) {
            super.loadUrl(url, mHttpHeaders)
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
        val headers = additionalHttpHeaders.ifEmpty { mHttpHeaders }
        loadUrl(finalUrl, headers)
    }

    /**
     * Checks whether a URL is permitted based on a set of permitted hostnames.
     * @param url The URL to check.
     * @return true if the URL is permitted, false otherwise.
     */
    fun isPermittedUrl(url: String?): Boolean {
        // If the URL is null or empty, it's not permitted
        if (url.isNullOrEmpty()) {
            return false
        }

        try {
            val parsedUrl = Uri.parse(url)

            // Get the hostname of the URL that is to be checked
            val actualHost = parsedUrl.host ?: return false

            // If the hostname could not be determined or contains invalid characters
            if (!actualHost.matches(Regex("^[a-zA-Z0-9._!~*')(;:&=+$,%\\[\\]-]*$"))) {
                // Prevent mismatches between interpretations by `Uri` and `WebView`
                return false
            }

            // Get the user information from the authority part of the URL that is to be checked
            val actualUserInformation = parsedUrl.userInfo

            // If the user information contains invalid characters
            if (actualUserInformation != null && !actualUserInformation.matches(Regex("^[a-zA-Z0-9._!~*')(;:&=+$,%-]*$"))) {
                // Prevent mismatches between interpretations by `Uri` and `WebView`
                return false
            }

            // For every hostname in the set of permitted hosts
            for (expectedHost in mPermittedHostnames) {
                // If the two hostnames match or if the actual host is a subdomain of the expected host
                if (actualHost == expectedHost || actualHost.endsWith(".$expectedHost")) {
                    // The actual hostname of the URL to be checked is allowed
                    return true
                }
            }
        } catch (e: Exception) {
            // Handle parsing exceptions or other errors
            e.printStackTrace()
        }

        // The actual hostname of the URL to be checked is not allowed since there were no matches
        return false
    }


    private fun setLastError() {
        mLastError = System.currentTimeMillis()
    }

    private fun hasError(): Boolean {
        return mLastError + 500 >= System.currentTimeMillis()
    }// return English translation by default

    /**
     * Provides localizations for the 25 most widely spoken languages that have an ISO 639-2/T code.
     * @return The label for the file upload prompts as a string.
     */
    private val fileUploadPromptLabel: String
        get() {
            try {
                return when (mLanguageIso3) {
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
                    else -> "Choose a file" // Return English translation by default
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
            return "Choose a file" // Return English translation by default
        }

    /**
     * Decodes a Base64 encoded string.
     * @param encoded The Base64 encoded string to decode.
     * @return The decoded string.
     */
    private fun decodeBase64(encoded: String): String {
        return String(
            android.util.Base64.decode(encoded, android.util.Base64.DEFAULT),
            Charsets.UTF_8
        )
    }

    private fun openFileInput(
        fileUploadCallbackFirst: ValueCallback<Uri?>?,
        fileUploadCallbackSecond: ValueCallback<Array<Uri?>?>?,
        allowMultiple: Boolean
    ) {
        // Clear any existing callbacks
        mFileUploadCallbackFirst?.onReceiveValue(null)
        mFileUploadCallbackFirst = fileUploadCallbackFirst
        mFileUploadCallbackSecond?.onReceiveValue(null)
        mFileUploadCallbackSecond = fileUploadCallbackSecond

        // Create intent to open file picker
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mUploadableFileTypes
            if (allowMultiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        // Start activity for result
        mActivity?.get()?.startActivityForResult(
            Intent.createChooser(intent, fileUploadPromptLabel),
            mRequestCodeFilePicker
        )
    }

    /**
     * Wrapper for methods related to alternative browsers that have their own rendering engines
     */
    object Browsers {
        /**
         * Package name of an alternative browser that is installed on this device
         */
        private var mAlternativePackage: String? = null

        /**
         * Returns whether there is an alternative browser with its own rendering engine currently installed
         *
         * @param context a valid `Context` reference
         * @return whether there is an alternative browser or not
         */
        fun hasAlternative(context: Context): Boolean {
            return getAlternative(context) != null
        }

        /**
         * Returns the package name of an alternative browser with its own rendering engine or `null`
         *
         * @param context a valid `Context` reference
         * @return the package name or `null`
         */
        fun getAlternative(context: Context): String? {
            mAlternativePackage?.let { return it }

            val alternativeBrowsers = ALTERNATIVE_BROWSERS.toList()
            val apps = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in apps) {
                if (!app.enabled) {
                    continue
                }
                if (alternativeBrowsers.contains(app.packageName)) {
                    mAlternativePackage = app.packageName
                    return app.packageName
                }
            }
            return null
        }

        /**
         * Opens the given URL in an alternative browser
         *
         * @param context           a valid `Activity` reference
         * @param url               the URL to open
         * @param withoutTransition whether to switch to the browser `Activity` without a transition
         */
        /**
         * Opens the given URL in an alternative browser
         *
         * @param context a valid `Activity` reference
         * @param url     the URL to open
         */
        @JvmOverloads
        fun openUrl(context: Activity, url: String?, withoutTransition: Boolean = false) {
            url?.let {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                intent.setPackage(getAlternative(context))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                if (withoutTransition) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        context.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    } else {
                        context.overridePendingTransition(0, 0)
                    }
                }
            }
        }

    }

    companion object {
        const val PACKAGE_NAME_DOWNLOAD_MANAGER = "com.android.providers.downloads"
        private const val REQUEST_CODE_FILE_PICKER = 51426
        private const val DATABASES_SUB_FOLDER = "/databases"
        private const val LANGUAGE_DEFAULT_ISO3 = "eng"
        private val CHARSET_DEFAULT = Charsets.UTF_8

        // Alternative browsers that have their own rendering engine
        private val ALTERNATIVE_BROWSERS = arrayOf(
            "org.mozilla.firefox",
            "com.android.chrome",
            "com.opera.browser",
            "org.mozilla.firefox_beta",
            "com.chrome.beta",
            "com.opera.browser.beta"
        )

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
                Locale.getDefault().isO3Language.toLowerCase(Locale.US)
            } catch (e: MissingResourceException) {
                LANGUAGE_DEFAULT_ISO3
            }

        @Throws(IllegalArgumentException::class, UnsupportedEncodingException::class)
        private fun decodeBase64(base64: String?): String {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            return String(bytes, CHARSET_DEFAULT)
        }

        /**
         * Returns whether file uploads can be used on the current device.
         */
        val isFileUploadAvailable: Boolean
            get() = isFileUploadAvailable(false)

        /**
         * Returns whether file uploads can be used on the current device.
         */
        private fun isFileUploadAvailable(needsCorrectMimeType: Boolean): Boolean {
            return (!needsCorrectMimeType )
        }

        /**
         * Handles a download by loading the file from `fromUrl` and saving it to `toFilename` on the external storage.
         */
        fun handleDownload(context: Context, fromUrl: String?, toFilename: String?): Boolean {
            val request = DownloadManager.Request(Uri.parse(fromUrl))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return try {
                dm.enqueue(request)
                true
            } catch (e: IllegalArgumentException) {
                // Show the settings screen where the user can enable the download manager app again
                openAppSettings(context, PACKAGE_NAME_DOWNLOAD_MANAGER)
                false
            }
        }

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