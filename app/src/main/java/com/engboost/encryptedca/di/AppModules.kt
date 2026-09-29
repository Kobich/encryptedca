package com.engboost.encryptedca.di

import com.engboost.encryptedca.core.certificates.impl.di.coreCertificatesModule
import com.engboost.encryptedca.core.network.impl.di.coreNetworkModule
import com.engboost.encryptedca.feature.certificates.impl.di.certificatesModule
import com.engboost.encryptedca.feature.scanner.impl.di.scannerModule
import com.engboost.encryptedca.feature.webpanel.impl.di.webPanelModule
import com.engboost.encryptedca.ui.certificates.impl.di.certificatesUiModule
import com.engboost.encryptedca.ui.scanner.impl.di.scannerUiModule
import com.engboost.encryptedca.ui.webpanel.impl.di.webPanelUiModule

val appModules = listOf(
    coreCertificatesModule,
    coreNetworkModule,
    certificatesModule,
    certificatesUiModule,
    scannerModule,
    scannerUiModule,
    webPanelModule,
    webPanelUiModule,
)
