package com.engboost.encryptedca.feature.webpanel

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.engboost.encryptedca.feature.webpanel.components.ImageMessageSnackbar
import com.engboost.encryptedca.feature.webpanel.components.ProblemText
import com.engboost.encryptedca.feature.webpanel.webview.PanelWebView
import com.engboost.encryptedca.feature.webpanel.webview.rememberPanelWebViewController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebPanelScreen(
    state: WebPanelState,
    onAction: (WebPanelAction) -> Unit,
    onClose: () -> Unit,
) {
    val panel = rememberPanelWebViewController()
    val snackbar = remember { SnackbarHostState() }

    BackHandler {
        if (!panel.goBack()) onClose()
    }

    ImageMessageSnackbar(
        message = state.imageMessage,
        snackbar = snackbar,
        onShown = { onAction(WebPanelAction.ImageMessageShown(it)) },
    )

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
                                onAction(WebPanelAction.Reload)
                                panel.reload()
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
                    controller = panel,
                    startUrl = state.startUrl,
                    host = state.host,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        panel.fullscreenVideo?.let { video ->
            AndroidView(factory = { video.view }, modifier = Modifier.fillMaxSize().background(Color.Black))
        }
    }
}
