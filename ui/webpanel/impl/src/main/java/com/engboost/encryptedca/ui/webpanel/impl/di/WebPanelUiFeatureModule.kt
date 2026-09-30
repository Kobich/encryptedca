package com.engboost.encryptedca.ui.webpanel.impl.di

import com.engboost.encryptedca.ui.webpanel.api.WebPanelUiFeature
import com.engboost.encryptedca.ui.webpanel.impl.WebPanelUiFeatureImpl
import com.engboost.encryptedca.ui.webpanel.impl.webpanel.WebPanelViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val webPanelUiFeatureModule = module {
    single<WebPanelUiFeature> { WebPanelUiFeatureImpl() }
    viewModel { params -> WebPanelViewModel(feature = get(), device = params.get()) }
}
