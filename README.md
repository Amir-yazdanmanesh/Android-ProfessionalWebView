# Professional WebView App

Professional WebView App is an Android application that provides a customizable WebView for browsing web content with advanced features.

## Features

- Customizable WebView with advanced settings.
- Handling special URLs such as telephone, email, and non-HTTP app links.
- First-party Jetpack Compose support via `ProfessionalWebView` composable.
- Automatic lifecycle management (resume, pause, destroy).
- File upload via `ActivityResultContracts`.
- Progress indicator for page loading.
- Error handling during WebView loading.

## Installation

Clone the repository to your local machine:

```bash
git clone https://github.com/Amir-yazdanmanesh/Android-ProfessionalWebView.git
```

### Permissions

Make sure to add the following permission to your AndroidManifest.xml file to allow internet access:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Usage

### Jetpack Compose (Recommended)

```kotlin
class MainActivity : AppCompatActivity(), WebViewClientListener {

    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val state = rememberProfessionalWebViewState()

                Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    ProfessionalWebView(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        onCreated = { webView ->
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
                    if (finished) finish()
                }
            }
        }
    }

    override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
        isLoading = true
    }

    override fun onPageFinished(webView: WebView, errorCode: Int, url: String?) {
        isLoading = false
    }

    // ... implement other WebViewClientListener callbacks
}
```

### XML Layout (View-based)

In your XML layout file, include the ProfessionalWebView:

```xml
<com.yazdanmanesh.professionalwebview.ProfessionalWebView
    android:id="@+id/browserWebView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

Then in your Activity:

```kotlin
class MainActivity : AppCompatActivity(), WebViewClientListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<ProfessionalWebView>(R.id.browserWebView)
        val specialUrlDetector = SpecialUrlDetectorImpl(this)
        val browserWebViewClient = BrowserWebViewClient(specialUrlDetector)
        browserWebViewClient.webViewClientListener = this
        webView.webViewClient = browserWebViewClient
        webView.loadUrl("https://www.google.com/")
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.onDestroy()
        super.onDestroy()
    }

    // ... implement WebViewClientListener callbacks
}
```

## Licenses

### Android-AdvancedWebView
This project uses Android-AdvancedWebView, which is licensed under the MIT License. The MIT License is a permissive license that allows you to use, modify, and distribute the software as long as you include the original copyright and license notice. See the [LICENSE-AdvancedWebView.md](https://github.com/delight-im/Android-AdvancedWebView?tab=MIT-1-ov-file#readme) file for more details.

### DuckDuckGo Android
This project includes classes from the DuckDuckGo Android repository, which are licensed under the Apache License 2.0. The Apache License 2.0 is a more complex license that provides extensive permissions and limitations for use, modification, and distribution of the software. See the [LICENSE-DuckDuckGo.md](https://github.com/duckduckgo/Android?tab=Apache-2.0-1-ov-file#readme) file for more details.


## Contribution

I've put a lot of effort into developing and maintaining this WebView app. Your contributions and stars are highly appreciated, as they motivate me to keep the project updated and share more knowledge with the community.
