package com.engboost.encryptedca.ui.webpanel.api

object WebPanelNavRoute {
    const val HOST_ARG = "host"
    const val FINGERPRINT_ARG = "fingerprint"
    const val ROUTE = "web-panel/{$HOST_ARG}/{$FINGERPRINT_ARG}"

    fun build(host: String, serverFingerprint: String) = "web-panel/$host/$serverFingerprint"
}
