package com.engboost.encryptedca.ui.webpanel.impl.di

import com.engboost.encryptedca.ui.webpanel.api.WebPanelUiFeature
import com.engboost.encryptedca.ui.webpanel.impl.WebPanelUiFeatureImpl
import com.engboost.encryptedca.ui.webpanel.impl.domain.WebPanelInteractor
import com.engboost.encryptedca.ui.webpanel.impl.ui.WebPanelViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val webPanelUiFeatureModule = module {
    single<WebPanelUiFeature> { WebPanelUiFeatureImpl() }
    factory { params -> WebPanelInteractor(webPanelFeature = get(), device = params.get()) }
    viewModel { params -> WebPanelViewModel(interactor = get { params }) }
}
