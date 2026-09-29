package com.engboost.encryptedca.feature.webpanel.impl.di

import com.engboost.encryptedca.feature.webpanel.api.WebPanelInteractor
import com.engboost.encryptedca.feature.webpanel.impl.data.MediaStoreScreenshotStorage
import com.engboost.encryptedca.feature.webpanel.impl.data.ScreenshotStorage
import com.engboost.encryptedca.feature.webpanel.impl.interactor.DefaultWebPanelInteractor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val webPanelModule = module {
    factory<ScreenshotStorage> { MediaStoreScreenshotStorage(androidContext().contentResolver) }
    factory<WebPanelInteractor> { params ->
        DefaultWebPanelInteractor(repository = get(), screenshots = get(), pin = params.get())
    }
}
