package com.engboost.encryptedca.ui.webpanel.impl.di

import com.engboost.encryptedca.ui.webpanel.api.WebPanelUi
import com.engboost.encryptedca.ui.webpanel.impl.WebPanelUiImpl
import com.engboost.encryptedca.ui.webpanel.impl.webpanel.WebPanelViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val webPanelUiModule = module {
    single<WebPanelUi> { WebPanelUiImpl() }
    viewModel { params -> WebPanelViewModel(interactor = get { params }) }
}
