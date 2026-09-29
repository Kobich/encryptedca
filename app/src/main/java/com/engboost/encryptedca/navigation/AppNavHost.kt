package com.engboost.encryptedca.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.engboost.encryptedca.ui.certificates.api.CertificatesRoute
import com.engboost.encryptedca.ui.certificates.api.CertificatesUi
import com.engboost.encryptedca.ui.certificates.api.navigateToCertificates
import com.engboost.encryptedca.ui.scanner.api.ScannerRoute
import com.engboost.encryptedca.ui.scanner.api.ScannerUi
import com.engboost.encryptedca.ui.webpanel.api.WebPanelRoute
import com.engboost.encryptedca.ui.webpanel.api.WebPanelUi
import com.engboost.encryptedca.ui.webpanel.api.navigateToWebPanel
import org.koin.compose.koinInject

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val scannerUi: ScannerUi = koinInject()
    val certificatesUi: CertificatesUi = koinInject()
    val webPanelUi: WebPanelUi = koinInject()

    NavHost(navController = navController, startDestination = ScannerRoute.ROUTE) {
        composable(ScannerRoute.ROUTE) {
            scannerUi.Content(
                onOpenCertificates = navController::navigateToCertificates,
                onOpenDevice = navController::navigateToWebPanel,
            )
        }
        composable(CertificatesRoute.ROUTE) {
            certificatesUi.Content(onBack = { navController.popBackStack() })
        }
        composable(WebPanelRoute.ROUTE) { entry ->
            webPanelUi.Content(
                host = requireNotNull(entry.arguments?.getString(WebPanelRoute.HOST_ARG)),
                serverFingerprint = requireNotNull(entry.arguments?.getString(WebPanelRoute.FINGERPRINT_ARG)),
                onClose = { navController.popBackStack() },
            )
        }
    }
}
