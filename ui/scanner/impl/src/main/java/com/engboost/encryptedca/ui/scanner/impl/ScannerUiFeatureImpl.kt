package com.engboost.encryptedca.ui.scanner.impl

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.scanner.api.ScannerUiFeature
import com.engboost.encryptedca.ui.scanner.impl.ui.ScannerScreen

internal class ScannerUiFeatureImpl : ScannerUiFeature {
    @Composable
    override fun Content(navController: NavHostController) {
        ScannerScreen(navController = navController)
    }
}
