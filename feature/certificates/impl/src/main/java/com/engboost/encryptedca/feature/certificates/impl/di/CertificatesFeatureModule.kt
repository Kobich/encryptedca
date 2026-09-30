package com.engboost.encryptedca.feature.certificates.impl.di

import com.engboost.encryptedca.feature.certificates.api.CertificatesFeature
import com.engboost.encryptedca.feature.certificates.impl.CertificatesFeatureImpl
import com.engboost.encryptedca.feature.certificates.impl.data.DocumentRepositoryImpl
import com.engboost.encryptedca.feature.certificates.impl.data.QrImageRepositoryImpl
import com.engboost.encryptedca.feature.certificates.impl.domain.CertificatesInteractor
import com.engboost.encryptedca.feature.certificates.impl.domain.DocumentRepository
import com.engboost.encryptedca.feature.certificates.impl.domain.QrImageRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val certificatesFeatureModule = module {
    factory<DocumentRepository> { DocumentRepositoryImpl(androidContext().contentResolver) }
    factory<QrImageRepository> { QrImageRepositoryImpl(androidContext()) }
    factory {
        CertificatesInteractor(profileStorage = get(), documentRepository = get(), qrImageRepository = get())
    }
    single<CertificatesFeature> { CertificatesFeatureImpl(interactor = get()) }
}
