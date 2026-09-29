// The device panel's WebView. Pauses the page while the app is in the background
// and shows full-screen video over the panel.
// The screen uses PanelWebViewController for the back and reload buttons.
package com.engboost.encryptedca.feature.webpanel.ui.impl.webview

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.engboost.encryptedca.feature.webpanel.impl.presentation.WebPanelAction

internal class FullscreenVideo(val view: View, val callback: WebChromeClient.CustomViewCallback)

@Stable
internal class PanelWebViewController {
    var webView by mutableStateOf<WebView?>(null)
    var fullscreenVideo by mutableStateOf<FullscreenVideo?>(null)

    fun reload() {
        webView?.reloadAskingForCertificate()
    }

    fun goBack(): Boolean {
        val video = fullscreenVideo
        val page = webView
        return when {
            video != null -> {
                video.callback.onCustomViewHidden()
                true
            }
            page != null && page.canGoBack() -> {
                page.goBack()
                true
            }
            else -> false
        }
    }
}

@Composable
internal fun rememberPanelWebViewController(): PanelWebViewController = remember { PanelWebViewController() }

@Composable
internal fun PanelWebView(
    controller: PanelWebViewController,
    startUrl: String,
    host: String,
    onAction: (WebPanelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller.webView) {
        val view = controller.webView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { context ->
            createPanelWebView(
                context = context,
                host = host,
                onAction = onAction,
                onFullscreenChange = { controller.fullscreenVideo = it },
            ).also { view ->
                controller.webView = view
                view.reloadAskingForCertificate(startUrl)
            }
        },
        onRelease = { it.destroy() },
        modifier = modifier,
    )
}
