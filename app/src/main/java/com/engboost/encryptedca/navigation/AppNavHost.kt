package com.engboost.encryptedca.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.engboost.encryptedca.ui.addprofile.api.AddProfileNavRoute
import com.engboost.encryptedca.ui.addprofile.api.AddProfileUiFeature
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource
import com.engboost.encryptedca.ui.profiles.api.ProfilesNavRoute
import com.engboost.encryptedca.ui.profiles.api.ProfilesUiFeature
import com.engboost.encryptedca.ui.scanner.api.ScannerNavRoute
import com.engboost.encryptedca.ui.scanner.api.ScannerUiFeature
import com.engboost.encryptedca.ui.webpanel.api.WebPanelNavRoute
import com.engboost.encryptedca.ui.webpanel.api.WebPanelUiFeature
import org.koin.compose.koinInject

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val scannerUi: ScannerUiFeature = koinInject()
    val profilesUi: ProfilesUiFeature = koinInject()
    val addProfileUi: AddProfileUiFeature = koinInject()
    val webPanelUi: WebPanelUiFeature = koinInject()

    NavHost(navController = navController, startDestination = ScannerNavRoute.ROUTE) {
        composable(ScannerNavRoute.ROUTE) {
            scannerUi.Content(navController)
        }
        composable(ProfilesNavRoute.ROUTE) {
            profilesUi.Content(navController)
        }
        composable(AddProfileNavRoute.ROUTE) { entry ->
            val source = requireNotNull(entry.arguments?.getString(AddProfileNavRoute.SOURCE_ARG))
            addProfileUi.Content(navController, ProfileSource.valueOf(source))
        }
        composable(WebPanelNavRoute.ROUTE) { entry ->
            webPanelUi.Content(
                navController = navController,
                host = requireNotNull(entry.arguments?.getString(WebPanelNavRoute.HOST_ARG)),
                serverFingerprint = requireNotNull(entry.arguments?.getString(WebPanelNavRoute.FINGERPRINT_ARG)),
            )
        }
    }
}
