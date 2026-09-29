package com.engboost.encryptedca.feature.webpanel.ui.impl.di

import com.engboost.encryptedca.feature.webpanel.ui.api.WebPanelUi
import com.engboost.encryptedca.feature.webpanel.ui.impl.WebPanelUiImpl
import org.koin.dsl.module

val webPanelUiModule = module {
    single<WebPanelUi> { WebPanelUiImpl() }
}
