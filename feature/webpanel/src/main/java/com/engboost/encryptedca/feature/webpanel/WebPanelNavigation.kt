package com.engboost.encryptedca.feature.webpanel

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository

private const val HOST_ARG = "host"
private const val FINGERPRINT_ARG = "fingerprint"
private const val WEB_PANEL_ROUTE = "web-panel/{$HOST_ARG}/{$FINGERPRINT_ARG}"

fun NavController.navigateToWebPanel(host: String, serverFingerprint: String) =
    navigate("web-panel/$host/$serverFingerprint")

fun NavGraphBuilder.webPanelDestination(
    repository: CertificateProfileRepository,
    onClose: () -> Unit,
) {
    composable(WEB_PANEL_ROUTE) { entry ->
        val host = requireNotNull(entry.arguments?.getString(HOST_ARG))
        val fingerprint = requireNotNull(entry.arguments?.getString(FINGERPRINT_ARG))
        val resolver = LocalContext.current.applicationContext.contentResolver
        val viewModel = viewModel { WebPanelViewModel(repository, ImageSaver(resolver), host, fingerprint) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        WebPanelScreen(state = state, onAction = viewModel::onAction, onClose = onClose)
    }
}
