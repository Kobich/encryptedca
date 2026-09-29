package com.engboost.encryptedca.feature.scanner.ui.impl.di

import com.engboost.encryptedca.feature.scanner.ui.api.ScannerUi
import com.engboost.encryptedca.feature.scanner.ui.impl.ScannerUiImpl
import org.koin.dsl.module

val scannerUiModule = module {
    single<ScannerUi> { ScannerUiImpl() }
}
