package com.engboost.encryptedca.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.engboost.encryptedca.EncryptedCaApplication
import com.engboost.encryptedca.feature.certificates.certificatesGraph
import com.engboost.encryptedca.feature.certificates.navigateToCertificates
import com.engboost.encryptedca.feature.scanner.SCANNER_ROUTE
import com.engboost.encryptedca.feature.scanner.scannerScreen
import com.engboost.encryptedca.feature.webpanel.navigateToWebPanel
import com.engboost.encryptedca.feature.webpanel.webPanelScreen

@Composable
fun AppNavHost() {
    val repository = (LocalContext.current.applicationContext as EncryptedCaApplication).certificateProfiles
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SCANNER_ROUTE) {
        scannerScreen(
            repository = repository,
            onOpenCertificates = navController::navigateToCertificates,
            onOpenDevice = navController::navigateToWebPanel,
        )
        webPanelScreen(repository = repository, onClose = { navController.popBackStack() })
        certificatesGraph(navController = navController, repository = repository)
    }
}
