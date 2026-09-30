package com.engboost.encryptedca.ui.certificates.impl.di

import com.engboost.encryptedca.ui.certificates.api.CertificatesUiFeature
import com.engboost.encryptedca.ui.certificates.impl.CertificatesUiFeatureImpl
import com.engboost.encryptedca.ui.certificates.impl.add.AddProfileViewModel
import com.engboost.encryptedca.ui.certificates.impl.list.ProfileListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val certificatesUiFeatureModule = module {
    single<CertificatesUiFeature> { CertificatesUiFeatureImpl() }
    viewModel { ProfileListViewModel(feature = get()) }
    viewModel { params -> AddProfileViewModel(feature = get(), savedState = get(), source = params.get()) }
}
