package com.engboost.encryptedca.ui.addprofile.impl.di

import com.engboost.encryptedca.ui.addprofile.api.AddProfileUiFeature
import com.engboost.encryptedca.ui.addprofile.impl.AddProfileUiFeatureImpl
import com.engboost.encryptedca.ui.addprofile.impl.domain.AddProfileInteractor
import com.engboost.encryptedca.ui.addprofile.impl.ui.AddProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val addProfileUiFeatureModule = module {
    single<AddProfileUiFeature> { AddProfileUiFeatureImpl() }
    factory { params -> AddProfileInteractor(certificatesFeature = get(), source = params.get()) }
    viewModel { params -> AddProfileViewModel(interactor = get { params }, savedState = get()) }
}
