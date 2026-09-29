package com.engboost.encryptedca.feature.webpanel

import android.net.Uri
import android.webkit.WebView
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
import com.engboost.encryptedca.core.certificates.model.ClientCredentials
import java.security.cert.X509Certificate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebPanelScreen(
    state: WebPanelState,
    onReload: () -> Unit,
    onPageStarted: (url: String) -> Unit,
    onPageFinished: () -> Unit,
    onClientCertRequest: (host: String, answer: (ClientCredentials?) -> Unit) -> Unit,
    onSslError: (url: String, certificate: X509Certificate?) -> Boolean,
    onDownload: (url: String) -> Unit,
    onImageMessageShown: (ImageMessage) -> Unit,
    onClose: () -> Unit,
) {
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

    ImageMessageSnackbar(state.imageMessage, snackbar, onImageMessageShown)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(state.pageUrl, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.web_panel_close))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onReload()
                                webView?.reloadAskingForCertificate()
                            },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.web_panel_reload))
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.problem?.let { ProblemText(it) }
                PanelWebView(
                    startUrl = state.startUrl,
                    host = state.host,
                    onPageStarted = onPageStarted,
                    onPageFinished = onPageFinished,
                    onClientCertRequest = onClientCertRequest,
                    onSslError = onSslError,
                    onDownload = onDownload,
                    onFullscreenChange = { fullscreen = it },
                    onCreated = { webView = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        fullscreen?.let { video ->
            AndroidView(factory = { video.view }, modifier = Modifier.fillMaxSize().background(Color.Black))
        }
    }
}

@Composable
private fun ProblemText(problem: WebPanelProblem) {
    Text(
        text = stringResource(
            when (problem) {
                WebPanelProblem.NO_PROFILE -> R.string.web_panel_no_profile
                WebPanelProblem.PROFILE_UNAVAILABLE -> R.string.web_panel_profile_unavailable
                WebPanelProblem.UNTRUSTED_SERVER -> R.string.web_panel_untrusted_server
            },
        ),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp),
    )
}

/** Shows each [ImageMessage] once; a newer message cancels the one on screen and takes its place. */
@Composable
private fun ImageMessageSnackbar(
    message: ImageMessage?,
    snackbar: SnackbarHostState,
    onShown: (ImageMessage) -> Unit,
) {
    val context = LocalContext.current
    val savedText = stringResource(R.string.web_panel_image_saved)
    val openLabel = stringResource(R.string.web_panel_image_open)
    val failedText = stringResource(R.string.web_panel_image_failed)
    LaunchedEffect(message) {
        when (message) {
            null -> return@LaunchedEffect
            is ImageMessage.Saved -> {
                val result = snackbar.showSnackbar(savedText, openLabel, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) {
                    openExternally(context, Uri.parse(message.uri), type = "image/*")
                }
            }
            is ImageMessage.Failed -> snackbar.showSnackbar(failedText)
        }
        onShown(message)
    }
}
