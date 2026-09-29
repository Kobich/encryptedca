package com.engboost.encryptedca.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.engboost.encryptedca.feature.certificates.api.CertificatesRoute
import com.engboost.encryptedca.feature.certificates.api.navigateToCertificates
import com.engboost.encryptedca.feature.certificates.ui.api.CertificatesUi
import com.engboost.encryptedca.feature.scanner.api.ScannerRoute
import com.engboost.encryptedca.feature.scanner.ui.api.ScannerUi
import com.engboost.encryptedca.feature.webpanel.api.WebPanelRoute
import com.engboost.encryptedca.feature.webpanel.api.navigateToWebPanel
import com.engboost.encryptedca.feature.webpanel.ui.api.WebPanelUi
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
