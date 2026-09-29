package com.engboost.encryptedca.ui.webpanel.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.engboost.encryptedca.feature.webpanel.api.model.DevicePin
import com.engboost.encryptedca.ui.webpanel.api.WebPanelUi
import com.engboost.encryptedca.ui.webpanel.impl.webpanel.WebPanelScreen
import com.engboost.encryptedca.ui.webpanel.impl.webpanel.WebPanelViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

internal class WebPanelUiImpl : WebPanelUi {

    @Composable
    override fun Content(host: String, serverFingerprint: String, onClose: () -> Unit) {
        val viewModel: WebPanelViewModel = koinViewModel { parametersOf(DevicePin(host, serverFingerprint)) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        WebPanelScreen(state = state, onAction = viewModel::onAction, onClose = onClose)
    }
}
