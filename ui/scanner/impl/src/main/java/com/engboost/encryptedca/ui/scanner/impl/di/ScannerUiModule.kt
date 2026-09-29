package com.engboost.encryptedca.ui.scanner.impl.di

import com.engboost.encryptedca.ui.scanner.api.ScannerUi
import com.engboost.encryptedca.ui.scanner.impl.ScannerUiImpl
import com.engboost.encryptedca.ui.scanner.impl.scanner.ScannerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val scannerUiModule = module {
    single<ScannerUi> { ScannerUiImpl() }
    viewModel { ScannerViewModel(interactor = get()) }
}
