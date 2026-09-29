package com.engboost.encryptedca.feature.webpanel.api

import androidx.navigation.NavController

object WebPanelRoute {
    const val HOST_ARG = "host"
    const val FINGERPRINT_ARG = "fingerprint"
    const val ROUTE = "web-panel/{$HOST_ARG}/{$FINGERPRINT_ARG}"

    fun of(host: String, serverFingerprint: String) = "web-panel/$host/$serverFingerprint"
}

fun NavController.navigateToWebPanel(host: String, serverFingerprint: String) =
    navigate(WebPanelRoute.of(host, serverFingerprint))
