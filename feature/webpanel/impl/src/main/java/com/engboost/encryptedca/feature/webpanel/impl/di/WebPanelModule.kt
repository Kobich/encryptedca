package com.engboost.encryptedca.feature.webpanel.impl.di

import com.engboost.encryptedca.feature.webpanel.impl.data.MediaStoreScreenshotStorage
import com.engboost.encryptedca.feature.webpanel.impl.domain.interactor.WebPanelInteractor
import com.engboost.encryptedca.feature.webpanel.impl.domain.repository.ScreenshotStorage
import com.engboost.encryptedca.feature.webpanel.impl.presentation.WebPanelViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val webPanelModule = module {
    factory<ScreenshotStorage> { MediaStoreScreenshotStorage(androidContext().contentResolver) }
    factory { params -> WebPanelInteractor(repository = get(), screenshots = get(), pin = params.get()) }
    viewModel { params -> WebPanelViewModel(interactor = get { params }) }
}
