package com.engboost.encryptedca.ui.scanner.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.engboost.encryptedca.ui.scanner.api.ScannerUiFeature
import com.engboost.encryptedca.ui.scanner.impl.scanner.ScannerScreen
import com.engboost.encryptedca.ui.scanner.impl.scanner.ScannerViewModel
import org.koin.androidx.compose.koinViewModel

internal class ScannerUiFeatureImpl : ScannerUiFeature {

    @Composable
    override fun Content(
        onOpenCertificates: () -> Unit,
        onOpenDevice: (host: String, serverFingerprint: String) -> Unit,
    ) {
        val viewModel: ScannerViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ScannerScreen(
            state = state,
            onRescan = viewModel::rescan,
            onOpenCertificates = onOpenCertificates,
            onOpenDevice = onOpenDevice,
        )
    }
}
