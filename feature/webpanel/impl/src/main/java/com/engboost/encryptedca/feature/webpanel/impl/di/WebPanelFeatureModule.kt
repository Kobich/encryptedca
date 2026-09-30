package com.engboost.encryptedca.feature.webpanel.impl.di

import com.engboost.encryptedca.feature.webpanel.api.WebPanelFeature
import com.engboost.encryptedca.feature.webpanel.impl.WebPanelFeatureImpl
import com.engboost.encryptedca.feature.webpanel.impl.data.ScreenshotRepositoryImpl
import com.engboost.encryptedca.feature.webpanel.impl.domain.ScreenshotRepository
import com.engboost.encryptedca.feature.webpanel.impl.domain.WebPanelInteractor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val webPanelFeatureModule = module {
    factory<ScreenshotRepository> { ScreenshotRepositoryImpl(androidContext().contentResolver) }
    factory { WebPanelInteractor(profileStorage = get(), screenshotRepository = get()) }
    single<WebPanelFeature> { WebPanelFeatureImpl(interactor = get()) }
}
