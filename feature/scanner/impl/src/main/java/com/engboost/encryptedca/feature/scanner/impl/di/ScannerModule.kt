package com.engboost.encryptedca.feature.scanner.impl.di

import com.engboost.encryptedca.feature.scanner.impl.domain.interactor.ScannerInteractor
import com.engboost.encryptedca.feature.scanner.impl.presentation.ScannerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val scannerModule = module {
    factory { ScannerInteractor(repository = get(), wifiMonitor = get(), scanner = get(), sslContexts = get()) }
    viewModel { ScannerViewModel(interactor = get()) }
}
