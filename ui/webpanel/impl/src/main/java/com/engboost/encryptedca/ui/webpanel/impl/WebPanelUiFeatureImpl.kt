package com.engboost.encryptedca.ui.webpanel.impl

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.engboost.encryptedca.feature.webpanel.api.entity.DevicePin
import com.engboost.encryptedca.ui.webpanel.api.WebPanelUiFeature
import com.engboost.encryptedca.ui.webpanel.impl.ui.WebPanelScreen

internal class WebPanelUiFeatureImpl : WebPanelUiFeature {
    @Composable
    override fun Content(navController: NavHostController, host: String, serverFingerprint: String) {
        WebPanelScreen(navController = navController, device = DevicePin(host, serverFingerprint))
    }
}
