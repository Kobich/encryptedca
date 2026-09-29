package com.engboost.encryptedca.feature.certificates.impl.di

import com.engboost.encryptedca.feature.certificates.impl.data.ContentResolverDocumentRepository
import com.engboost.encryptedca.feature.certificates.impl.data.MlKitQrImageReader
import com.engboost.encryptedca.feature.certificates.impl.domain.interactor.AddProfileInteractor
import com.engboost.encryptedca.feature.certificates.impl.domain.interactor.ProfileListInteractor
import com.engboost.encryptedca.feature.certificates.impl.domain.repository.DocumentRepository
import com.engboost.encryptedca.feature.certificates.impl.domain.repository.QrImageReader
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.AddProfileViewModel
import com.engboost.encryptedca.feature.certificates.impl.presentation.list.ProfileListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val certificatesModule = module {
    factory<DocumentRepository> { ContentResolverDocumentRepository(androidContext().contentResolver) }
    factory<QrImageReader> { MlKitQrImageReader(androidContext()) }

    factory { ProfileListInteractor(repository = get()) }
    factory { AddProfileInteractor(repository = get(), documents = get(), qrImages = get()) }

    viewModel { ProfileListViewModel(interactor = get()) }
    viewModel { params -> AddProfileViewModel(interactor = get(), savedState = get(), source = params.get()) }
}
