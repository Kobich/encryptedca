package com.engboost.encryptedca.feature.scanner.impl.di

import com.engboost.encryptedca.feature.scanner.api.ScannerFeature
import com.engboost.encryptedca.feature.scanner.impl.ScannerFeatureImpl
import com.engboost.encryptedca.feature.scanner.impl.domain.ScannerInteractor
import org.koin.dsl.module

val scannerFeatureModule = module {
    // factory: the interactor has no state, and only the single ScannerFeatureImpl takes it,
    // so it is created once anyway.
    factory {
        ScannerInteractor(profileStorage = get(), network = get())
    }
    single<ScannerFeature> { ScannerFeatureImpl(interactor = get()) }
}
