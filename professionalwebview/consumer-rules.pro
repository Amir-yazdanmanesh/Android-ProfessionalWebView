# Keep public API classes
-keep class com.yazdanmanesh.professionalwebview.ProfessionalWebView { public *; }
-keep class com.yazdanmanesh.professionalwebview.BrowserWebViewClient { public *; }
-keep class com.yazdanmanesh.professionalwebview.WebViewClientListener { *; }
-keep class com.yazdanmanesh.professionalwebview.SpecialUrlDetector { *; }
-keep class com.yazdanmanesh.professionalwebview.SpecialUrlDetectorImpl { public *; }

# Keep UrlType sealed class hierarchy
-keep class com.yazdanmanesh.professionalwebview.SpecialUrlDetector$UrlType { *; }
-keep class com.yazdanmanesh.professionalwebview.SpecialUrlDetector$UrlType$* { *; }

# Keep DeeplinkConfig data class
-keep class com.yazdanmanesh.professionalwebview.SpecialUrlDetectorImpl$DeeplinkConfig { *; }

# Keep Compose wrapper
-keep class com.yazdanmanesh.professionalwebview.ProfessionalWebViewState { *; }
-keep class com.yazdanmanesh.professionalwebview.ProfessionalWebViewComposableKt { *; }

# Keep WebView JS interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
