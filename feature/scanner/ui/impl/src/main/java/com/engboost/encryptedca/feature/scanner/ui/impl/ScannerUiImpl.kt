package com.engboost.encryptedca.feature.scanner.ui.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.engboost.encryptedca.feature.scanner.impl.presentation.ScannerViewModel
import com.engboost.encryptedca.feature.scanner.ui.api.ScannerUi
import org.koin.androidx.compose.koinViewModel

internal class ScannerUiImpl : ScannerUi {

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
