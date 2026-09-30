package com.engboost.encryptedca.ui.webpanel.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.engboost.encryptedca.feature.webpanel.api.entity.DevicePin
import com.engboost.encryptedca.ui.webpanel.impl.ui.entity.WebPanelCallbacks
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun WebPanelScreen(
    navController: NavHostController,
    device: DevicePin,
    vm: WebPanelViewModel = koinViewModel { parametersOf(device) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val callbacks = remember(vm, navController) {
        WebPanelCallbacks(
            onClose = { navController.popBackStack() },
            onReload = vm::reload,
            onPageStarted = vm::onPageStarted,
            onPageFinished = vm::onPageFinished,
            onDownloadRequested = vm::saveScreenshot,
            onImageMessageShown = vm::onImageMessageShown,
            onClientCertRequested = vm::answerClientCert,
            isTrustedServer = vm::isTrustedServer,
        )
    }
    WebPanelScreenView(state = state, callbacks = callbacks)
}
