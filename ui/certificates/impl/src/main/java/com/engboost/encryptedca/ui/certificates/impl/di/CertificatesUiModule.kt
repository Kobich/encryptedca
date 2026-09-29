package com.engboost.encryptedca.ui.certificates.impl.di

import com.engboost.encryptedca.ui.certificates.api.CertificatesUi
import com.engboost.encryptedca.ui.certificates.impl.CertificatesUiImpl
import com.engboost.encryptedca.ui.certificates.impl.add.AddProfileViewModel
import com.engboost.encryptedca.ui.certificates.impl.list.ProfileListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val certificatesUiModule = module {
    single<CertificatesUi> { CertificatesUiImpl() }
    viewModel { ProfileListViewModel(interactor = get()) }
    viewModel { params -> AddProfileViewModel(interactor = get(), savedState = get(), source = params.get()) }
}
