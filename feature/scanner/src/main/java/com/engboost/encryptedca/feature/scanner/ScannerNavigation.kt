package com.engboost.encryptedca.feature.scanner

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.network.DeviceScanner

const val SCANNER_ROUTE = "scanner"

fun NavGraphBuilder.scannerScreen(
    repository: CertificateProfileRepository,
    onOpenCertificates: () -> Unit,
    onOpenDevice: (ip: String, serverFingerprint: String) -> Unit,
) {
    composable(SCANNER_ROUTE) {
        val context = LocalContext.current
        val viewModel = viewModel { ScannerViewModel(repository, DeviceScanner(context.applicationContext)) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        ScannerScreen(
            state = state,
            onRescan = viewModel::rescan,
            onOpenCertificates = onOpenCertificates,
            onOpenDevice = onOpenDevice,
        )
    }
}
