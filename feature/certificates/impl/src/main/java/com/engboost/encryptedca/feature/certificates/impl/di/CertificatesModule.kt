package com.engboost.encryptedca.feature.certificates.impl.di

import com.engboost.encryptedca.feature.certificates.api.AddProfileInteractor
import com.engboost.encryptedca.feature.certificates.api.ProfileListInteractor
import com.engboost.encryptedca.feature.certificates.impl.data.ContentResolverDocumentRepository
import com.engboost.encryptedca.feature.certificates.impl.data.DocumentRepository
import com.engboost.encryptedca.feature.certificates.impl.data.MlKitQrImageReader
import com.engboost.encryptedca.feature.certificates.impl.data.QrImageReader
import com.engboost.encryptedca.feature.certificates.impl.interactor.DefaultAddProfileInteractor
import com.engboost.encryptedca.feature.certificates.impl.interactor.DefaultProfileListInteractor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val certificatesModule = module {
    factory<DocumentRepository> { ContentResolverDocumentRepository(androidContext().contentResolver) }
    factory<QrImageReader> { MlKitQrImageReader(androidContext()) }
    factory<ProfileListInteractor> { DefaultProfileListInteractor(repository = get()) }
    factory<AddProfileInteractor> {
        DefaultAddProfileInteractor(repository = get(), documents = get(), qrImages = get())
    }
}
