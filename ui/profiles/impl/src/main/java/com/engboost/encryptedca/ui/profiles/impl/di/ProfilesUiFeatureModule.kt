package com.engboost.encryptedca.ui.profiles.impl.di

import com.engboost.encryptedca.ui.profiles.api.ProfilesUiFeature
import com.engboost.encryptedca.ui.profiles.impl.ProfilesUiFeatureImpl
import com.engboost.encryptedca.ui.profiles.impl.domain.ProfilesInteractor
import com.engboost.encryptedca.ui.profiles.impl.ui.ProfilesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profilesUiFeatureModule = module {
    single<ProfilesUiFeature> { ProfilesUiFeatureImpl() }
    factory { ProfilesInteractor(certificatesFeature = get()) }
    viewModel { ProfilesViewModel(interactor = get()) }
}
