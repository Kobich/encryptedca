package com.engboost.encryptedca.core.certificates.impl.di

import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.impl.DefaultCertificateProfileRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreCertificatesModule = module {
    single<CertificateProfileRepository> { DefaultCertificateProfileRepository(androidContext()) }
}
