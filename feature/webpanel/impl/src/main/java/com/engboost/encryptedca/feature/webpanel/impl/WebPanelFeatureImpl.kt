package com.engboost.encryptedca.feature.webpanel.impl

import android.net.Uri
import com.engboost.encryptedca.feature.webpanel.api.WebPanelFeature
import com.engboost.encryptedca.feature.webpanel.api.entity.ClientCertAnswer
import com.engboost.encryptedca.feature.webpanel.api.entity.DevicePin
import com.engboost.encryptedca.feature.webpanel.impl.domain.WebPanelInteractor
import java.security.cert.X509Certificate

internal class WebPanelFeatureImpl(
    private val interactor: WebPanelInteractor,
) : WebPanelFeature {
    override suspend fun answerClientCert(device: DevicePin, requestHost: String): ClientCertAnswer =
        interactor.answerClientCert(device, requestHost)

    override fun isTrustedServer(device: DevicePin, url: String, certificate: X509Certificate?): Boolean =
        interactor.isTrustedServer(device, url, certificate)

    override suspend fun saveScreenshot(dataUrl: String): Uri? = interactor.saveScreenshot(dataUrl)
}
