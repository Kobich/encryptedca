package com.engboost.encryptedca.ui.scanner.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

interface ScannerUiFeature {
    @Composable
    fun Content(navController: NavHostController)
}
