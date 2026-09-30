package com.engboost.encryptedca.ui.scanner.impl.di

import com.engboost.encryptedca.ui.scanner.api.ScannerUiFeature
import com.engboost.encryptedca.ui.scanner.impl.ScannerUiFeatureImpl
import com.engboost.encryptedca.ui.scanner.impl.domain.ScannerInteractor
import com.engboost.encryptedca.ui.scanner.impl.ui.ScannerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val scannerUiFeatureModule = module {
    single<ScannerUiFeature> { ScannerUiFeatureImpl() }
    factory { ScannerInteractor(scannerFeature = get()) }
    viewModel { ScannerViewModel(interactor = get()) }
}
