// Настройки WebView и обработчики его событий.
// Ссылки на другие хосты открываются в браузере: чужие страницы не должны получить клиентский ключ.
// WebView запоминает ответ на запрос клиентского сертификата. После смены профиля или отказа
// этот ответ сбрасывается, чтобы устройство спросило сертификат заново.
// Размер MATCH_PARENT задан явно: иначе WebView подгоняет окно под содержимое и ломается вёрстка на 100vh.
// Смешанный контент разрешён: страницы устройства грузят ресурсы и по http, и по https.
package com.engboost.encryptedca.feature.webpanel.webview

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
import com.engboost.encryptedca.feature.webpanel.WebPanelAction
import com.engboost.encryptedca.feature.webpanel.openExternally

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
