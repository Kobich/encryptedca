package com.engboost.encryptedca.feature.scanner.impl.di

import com.engboost.encryptedca.feature.scanner.api.ScannerInteractor
import com.engboost.encryptedca.feature.scanner.impl.interactor.DefaultScannerInteractor
import org.koin.dsl.module

val scannerModule = module {
    factory<ScannerInteractor> {
        DefaultScannerInteractor(repository = get(), wifiMonitor = get(), scanner = get(), sslContexts = get())
    }
}
