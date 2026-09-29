package com.engboost.encryptedca.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.feature.certificates.certificatesGraph
import com.engboost.encryptedca.feature.certificates.navigateToCertificates
import com.engboost.encryptedca.feature.scanner.SCANNER_ROUTE
import com.engboost.encryptedca.feature.scanner.scannerDestination
import com.engboost.encryptedca.feature.webpanel.navigateToWebPanel
import com.engboost.encryptedca.feature.webpanel.webPanelDestination

@Composable
fun AppNavHost(repository: CertificateProfileRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SCANNER_ROUTE) {
        scannerDestination(
            repository = repository,
            onOpenCertificates = navController::navigateToCertificates,
            onOpenDevice = navController::navigateToWebPanel,
        )
        webPanelDestination(repository = repository, onClose = { navController.popBackStack() })
        certificatesGraph(navController = navController, repository = repository)
    }
}
