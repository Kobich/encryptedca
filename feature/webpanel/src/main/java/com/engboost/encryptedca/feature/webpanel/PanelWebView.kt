package com.engboost.encryptedca.feature.webpanel

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.engboost.encryptedca.core.certificates.model.ClientCredentials
import java.security.cert.X509Certificate

/** A video the page switched to full screen; shown over the panel until the page or Back hides it. */
internal class FullscreenVideo(val view: View, val callback: WebChromeClient.CustomViewCallback)

/**
 * The device's page: WebView settings, callbacks and lifecycle. Whether to trust the device or hand it
 * the client key is decided by the caller; this only applies the answer to the platform request.
 */
@Composable
internal fun PanelWebView(
    startUrl: String,
    host: String,
    onPageStarted: (url: String) -> Unit,
    onPageFinished: () -> Unit,
    onClientCertRequest: (host: String, answer: (ClientCredentials?) -> Unit) -> Unit,
    onSslError: (url: String, certificate: X509Certificate?) -> Boolean,
    onDownload: (url: String) -> Unit,
    onFullscreenChange: (FullscreenVideo?) -> Unit,
    onCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Pauses the page's scripts and video while the app is in the background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, webView) {
        val view = webView
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
            createWebView(
                context = context,
                client = PanelWebViewClient(host, onPageStarted, onPageFinished, onClientCertRequest, onSslError),
                chrome = FullscreenChromeClient(onFullscreenChange),
                onDownload = onDownload,
            ).also { view ->
                webView = view
                onCreated(view)
                view.reloadAskingForCertificate(startUrl)
            }
        },
        onRelease = { it.destroy() },
        modifier = modifier,
    )
}

/**
 * WebView remembers its answer to a client-certificate request per host; after a profile change or
 * a refused request it would reuse it. Forgetting it makes the device ask again.
 */
internal fun WebView.reloadAskingForCertificate(url: String? = null) =
    WebView.clearClientCertPreferences { if (url != null) loadUrl(url) else reload() }

internal fun openExternally(context: Context, uri: Uri, type: String? = null) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, type)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing on the phone can open it; the panel stays as it is.
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    client: WebViewClient,
    chrome: WebChromeClient,
    onDownload: (String) -> Unit,
) = WebView(context).apply {
    // Without explicit MATCH_PARENT WebView sizes its viewport to the content, so 100vh layouts break.
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    settings.javaScriptEnabled = true
    // The device's pages mix https and http resources.
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    webViewClient = client
    webChromeClient = chrome
    setDownloadListener { downloadUrl, _, _, _, _ -> onDownload(downloadUrl) }
}

/** Keeps the panel on the device: other hosts open in the browser and never see the client key. */
private class PanelWebViewClient(
    private val host: String,
    private val pageStarted: (String) -> Unit,
    private val pageFinished: () -> Unit,
    private val clientCertRequested: (String, (ClientCredentials?) -> Unit) -> Unit,
    private val sslErrorAccepted: (String, X509Certificate?) -> Boolean,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host == host) return false
        openExternally(view.context, request.url)
        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = pageStarted(url)

    override fun onPageFinished(view: WebView, url: String) = pageFinished()

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) =
        clientCertRequested(request.host) { credentials ->
            if (credentials != null) {
                request.proceed(credentials.privateKey, credentials.certificateChain.toTypedArray())
            } else {
                request.cancel()
            }
        }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        if (sslErrorAccepted(error.url, error.certificate.x509Certificate)) handler.proceed() else handler.cancel()
    }
}

private class FullscreenChromeClient(private val onFullscreenChange: (FullscreenVideo?) -> Unit) : WebChromeClient() {
    override fun onShowCustomView(view: View, callback: CustomViewCallback) =
        onFullscreenChange(FullscreenVideo(view, callback))

    override fun onHideCustomView() = onFullscreenChange(null)
}
