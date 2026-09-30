package com.engboost.encryptedca.core.certificates.impl.di

import com.engboost.encryptedca.core.certificates.api.ProfileStorageFeature
import com.engboost.encryptedca.core.certificates.impl.ProfileStorageFeatureImpl
import com.engboost.encryptedca.core.certificates.impl.data.CaCertificateRepositoryImpl
import com.engboost.encryptedca.core.certificates.impl.data.ClientKeyRepositoryImpl
import com.engboost.encryptedca.core.certificates.impl.data.ProfileIndexRepositoryImpl
import com.engboost.encryptedca.core.certificates.impl.domain.CaCertificateRepository
import com.engboost.encryptedca.core.certificates.impl.domain.ClientKeyRepository
import com.engboost.encryptedca.core.certificates.impl.domain.ProfileIndexRepository
import com.engboost.encryptedca.core.certificates.impl.domain.ProfileStorageInteractor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val profileStorageFeatureModule = module {
    single<ClientKeyRepository> { ClientKeyRepositoryImpl() }
    single<CaCertificateRepository> { CaCertificateRepositoryImpl(androidContext()) }
    single<ProfileIndexRepository> { ProfileIndexRepositoryImpl(androidContext()) }
    single { ProfileStorageInteractor(keys = get(), caCertificates = get(), profiles = get()) }
    single<ProfileStorageFeature> { ProfileStorageFeatureImpl(interactor = get()) }
}
