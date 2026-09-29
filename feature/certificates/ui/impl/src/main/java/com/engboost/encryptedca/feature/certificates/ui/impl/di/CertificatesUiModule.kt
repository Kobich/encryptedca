package com.engboost.encryptedca.feature.certificates.ui.impl.di

import com.engboost.encryptedca.feature.certificates.ui.api.CertificatesUi
import com.engboost.encryptedca.feature.certificates.ui.impl.CertificatesUiImpl
import org.koin.dsl.module

val certificatesUiModule = module {
    single<CertificatesUi> { CertificatesUiImpl() }
}
