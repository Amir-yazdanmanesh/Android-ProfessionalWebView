package com.yazdanmanesh.professionalwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class ProfessionalWebViewState {
    var webView: ProfessionalWebView? by mutableStateOf(null)
        internal set
}

@Composable
fun rememberProfessionalWebViewState(): ProfessionalWebViewState {
    return remember { ProfessionalWebViewState() }
}

@Composable
fun ProfessionalWebView(
    state: ProfessionalWebViewState,
    modifier: Modifier = Modifier,
    onCreated: ((ProfessionalWebView) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            ProfessionalWebView(context).also { webView ->
                state.webView = webView
                onCreated?.invoke(webView)
            }
        },
        modifier = modifier,
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> state.webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> state.webView?.onPause()
                Lifecycle.Event.ON_DESTROY -> state.webView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
