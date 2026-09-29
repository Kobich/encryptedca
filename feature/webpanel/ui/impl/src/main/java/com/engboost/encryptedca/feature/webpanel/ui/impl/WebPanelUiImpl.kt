package com.engboost.encryptedca.feature.webpanel.ui.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.engboost.encryptedca.feature.webpanel.impl.domain.model.DevicePin
import com.engboost.encryptedca.feature.webpanel.impl.presentation.WebPanelViewModel
import com.engboost.encryptedca.feature.webpanel.ui.api.WebPanelUi
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
