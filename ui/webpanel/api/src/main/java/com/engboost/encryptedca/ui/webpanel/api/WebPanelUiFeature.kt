package com.engboost.encryptedca.ui.webpanel.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

interface WebPanelUiFeature {
    @Composable
    fun Content(navController: NavHostController, host: String, serverFingerprint: String)
}
