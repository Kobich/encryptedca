package com.engboost.encryptedca.feature.webpanel

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** A video the page switched to full screen, shown over everything until the page or Back hides it. */
private class FullscreenVideo(val view: View, val callback: WebChromeClient.CustomViewCallback)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebPanelScreen(
    state: WebPanelState,
    onPageStarted: (url: String) -> Unit,
    onPageFinished: () -> Unit,
    onClientCertRequest: (ClientCertRequest) -> Unit,
    onSslError: (SslErrorHandler, SslError) -> Unit,
    onDownload: (url: String) -> Unit,
    onMessageShown: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fullscreen by remember { mutableStateOf<FullscreenVideo?>(null) }
    val snackbar = remember { SnackbarHostState() }

    BackHandler {
        val video = fullscreen
        val page = webView
        when {
            video != null -> video.callback.onCustomViewHidden()
            page != null && page.canGoBack() -> page.goBack()
            else -> onClose()
        }
    }

    val savedMessage = stringResource(R.string.web_panel_image_saved)
    val openLabel = stringResource(R.string.web_panel_image_open)
    val failedMessage = stringResource(R.string.web_panel_image_failed)
    LaunchedEffect(state.savedImage, state.imageSaveFailed) {
        val image = state.savedImage
        if (image != null) {
            val result = snackbar.showSnackbar(savedMessage, openLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                openExternally(context, Uri.parse(image), type = "image/*")
            }
            onMessageShown()
        } else if (state.imageSaveFailed) {
            snackbar.showSnackbar(failedMessage)
            onMessageShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(state.url, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.web_panel_close))
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.web_panel_reload))
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.problem?.let { problem ->
                    Text(
                        text = stringResource(
                            when (problem) {
                                WebPanelProblem.NO_PROFILE -> R.string.web_panel_no_profile
                                WebPanelProblem.UNTRUSTED_SERVER -> R.string.web_panel_untrusted_server
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                AndroidView(
                    factory = { viewContext ->
                        createWebView(
                            context = viewContext,
                            url = state.url,
                            client = PanelWebViewClient(state.host, onPageStarted, onPageFinished, onClientCertRequest, onSslError),
                            chrome = object : WebChromeClient() {
                                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                    fullscreen = FullscreenVideo(view, callback)
                                }

                                override fun onHideCustomView() {
                                    fullscreen = null
                                }
                            },
                            onDownload = onDownload,
                        ).also { webView = it }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        fullscreen?.let { video ->
            AndroidView(factory = { video.view }, modifier = Modifier.fillMaxSize().background(Color.Black))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    url: String,
    client: WebViewClient,
    chrome: WebChromeClient,
    onDownload: (String) -> Unit,
) = WebView(context).apply {
    settings.javaScriptEnabled = true
    // The device's pages mix https and http resources.
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    webViewClient = client
    webChromeClient = chrome
    setDownloadListener { downloadUrl, _, _, _, _ -> onDownload(downloadUrl) }
    // WebView remembers which key it gave a host; after a profile change it would reuse the old one.
    WebView.clearClientCertPreferences { loadUrl(url) }
}

/** Keeps the panel on the device: other hosts open in the browser and never see the client key. */
private class PanelWebViewClient(
    private val host: String,
    private val pageStarted: (String) -> Unit,
    private val pageFinished: () -> Unit,
    private val clientCertRequested: (ClientCertRequest) -> Unit,
    private val sslErrorReceived: (SslErrorHandler, SslError) -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host == host) return false
        openExternally(view.context, request.url)
        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = pageStarted(url)

    override fun onPageFinished(view: WebView, url: String) = pageFinished()

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) = clientCertRequested(request)

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) = sslErrorReceived(handler, error)
}

private fun openExternally(context: Context, uri: Uri, type: String? = null) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, type)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing on the phone can open it; the panel stays as it is.
    }
}
