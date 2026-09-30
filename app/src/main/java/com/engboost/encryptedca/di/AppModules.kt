package com.engboost.encryptedca.di

import com.engboost.encryptedca.core.certificates.impl.di.profileStorageFeatureModule
import com.engboost.encryptedca.core.network.impl.di.networkFeatureModule
import com.engboost.encryptedca.feature.certificates.impl.di.certificatesFeatureModule
import com.engboost.encryptedca.feature.scanner.impl.di.scannerFeatureModule
import com.engboost.encryptedca.feature.webpanel.impl.di.webPanelFeatureModule
import com.engboost.encryptedca.ui.addprofile.impl.di.addProfileUiFeatureModule
import com.engboost.encryptedca.ui.profiles.impl.di.profilesUiFeatureModule
import com.engboost.encryptedca.ui.scanner.impl.di.scannerUiFeatureModule
import com.engboost.encryptedca.ui.webpanel.impl.di.webPanelUiFeatureModule

val appModules = listOf(
    profileStorageFeatureModule,
    networkFeatureModule,
    certificatesFeatureModule,
    scannerFeatureModule,
    webPanelFeatureModule,
    scannerUiFeatureModule,
    profilesUiFeatureModule,
    addProfileUiFeatureModule,
    webPanelUiFeatureModule,
)
