// WebView settings and event handlers.
// Links to other hosts open in the browser: other pages must never get the client key.
// WebView remembers its answer to a client-certificate request. After a profile change or a refusal
// that answer is cleared, so the device asks for the certificate again.
// MATCH_PARENT is set explicitly: otherwise WebView sizes itself to the content and 100vh layouts break.
// Mixed content is allowed: the device's pages load resources over both http and https.
package com.engboost.encryptedca.ui.webpanel.impl.webpanel.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import com.engboost.encryptedca.ui.webpanel.impl.common.openExternally
import com.engboost.encryptedca.ui.webpanel.impl.webpanel.WebPanelAction

internal fun WebView.reloadAskingForCertificate(url: String? = null) =
    WebView.clearClientCertPreferences { if (url != null) loadUrl(url) else reload() }

@SuppressLint("SetJavaScriptEnabled")
internal fun createPanelWebView(
    context: Context,
    host: String,
    onAction: (WebPanelAction) -> Unit,
    onFullscreenChange: (FullscreenVideo?) -> Unit,
) = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    settings.javaScriptEnabled = true
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    webViewClient = PanelWebViewClient(host, onAction)
    webChromeClient = FullscreenChromeClient(onFullscreenChange)
    setDownloadListener { url, _, _, _, _ -> onAction(WebPanelAction.DownloadRequested(url)) }
}

private class PanelWebViewClient(
    private val host: String,
    private val onAction: (WebPanelAction) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host == host) return false
        openExternally(view.context, request.url)
        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) =
        onAction(WebPanelAction.PageStarted(url))

    override fun onPageFinished(view: WebView, url: String) =
        onAction(WebPanelAction.PageFinished)

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) =
        onAction(
            WebPanelAction.ClientCertRequested(request.host) { credentials ->
                if (credentials != null) {
                    request.proceed(credentials.privateKey, credentials.certificateChain.toTypedArray())
                } else {
                    request.cancel()
                }
            },
        )

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) =
        onAction(
            WebPanelAction.ServerCertificateReceived(error.url, error.certificate.x509Certificate) { trusted ->
                if (trusted) handler.proceed() else handler.cancel()
            },
        )
}

private class FullscreenChromeClient(
    private val onFullscreenChange: (FullscreenVideo?) -> Unit,
) : WebChromeClient() {

    override fun onShowCustomView(view: View, callback: CustomViewCallback) =
        onFullscreenChange(FullscreenVideo(view, callback))

    override fun onHideCustomView() = onFullscreenChange(null)
}
