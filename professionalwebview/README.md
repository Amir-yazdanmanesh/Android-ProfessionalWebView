# Professional WebView App

Professional WebView App is an Android application that provides a customizable WebView for browsing web content with advanced features.

## Features

- Customizable WebView with advanced settings.
- Handling special URLs such as telephone, email, and non-HTTP app links.
- Progress bar for indicating page loading.
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

## XML Layout

In your XML layout file (e.g., activity_main.xml), include the ProfessionalWebView and ProgressBar as follows:

```xml
    <com.yazdanmanesh.professionalwebview.ProfessionalWebView
        android:id="@+id/browserWebView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

```

## Activity

In your MainActivity.kt file, initialize the Professional WebView and set up the WebViewClientListener as shown below:

```kotlin

class MainActivity : AppCompatActivity(), WebViewClientListener {
    lateinit var webView: ProfessionalWebView
    lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    
        // Initialize ProfessionalWebView
        val specialUrlDetectorImpl = SpecialUrlDetectorImpl(this)
        webView = findViewById(R.id.browserWebView)
        loading = findViewById(R.id.webview_loading)

        val browserWebViewClient = BrowserWebViewClient(specialUrlDetectorImpl)
        browserWebViewClient.webViewClientListener = this
        webView.let {
            it.webViewClient = browserWebViewClient
            navigate("https://example.com/")
        }
    }
}

```

## Licenses

### Android-AdvancedWebView
This project uses Android-AdvancedWebView, which is licensed under the MIT License. The MIT License is a permissive license that allows you to use, modify, and distribute the software as long as you include the original copyright and license notice. See the [LICENSE-AdvancedWebView.md](https://github.com/delight-im/Android-AdvancedWebView?tab=MIT-1-ov-file#readme) file for more details.

### DuckDuckGo Android Classes
This project includes classes from the DuckDuckGo Android repository, which are licensed under the Apache License 2.0. The Apache License 2.0 is a more complex license that provides extensive permissions and limitations for use, modification, and distribution of the software. See the [LICENSE-DuckDuckGo.md](https://github.com/duckduckgo/Android?tab=Apache-2.0-1-ov-file#readme) file for more details.


## Contribution

I've put a lot of effort into developing and maintaining this WebView app. Your contributions and stars are highly appreciated, as they motivate me to keep the project updated and share more knowledge with the community.