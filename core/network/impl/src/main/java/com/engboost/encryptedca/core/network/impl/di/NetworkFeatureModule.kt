package com.engboost.encryptedca.core.network.impl.di

import com.engboost.encryptedca.core.network.api.NetworkFeature
import com.engboost.encryptedca.core.network.impl.NetworkFeatureImpl
import com.engboost.encryptedca.core.network.impl.data.DeviceRepositoryImpl
import com.engboost.encryptedca.core.network.impl.data.SslContextRepositoryImpl
import com.engboost.encryptedca.core.network.impl.data.WifiRepositoryImpl
import com.engboost.encryptedca.core.network.impl.domain.DeviceRepository
import com.engboost.encryptedca.core.network.impl.domain.NetworkInteractor
import com.engboost.encryptedca.core.network.impl.domain.SslContextRepository
import com.engboost.encryptedca.core.network.impl.domain.WifiRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkFeatureModule = module {
    factory<WifiRepository> { WifiRepositoryImpl(androidContext()) }
    factory<DeviceRepository> { DeviceRepositoryImpl() }
    factory<SslContextRepository> { SslContextRepositoryImpl() }
    factory { NetworkInteractor(wifiRepository = get(), deviceRepository = get(), sslContextRepository = get()) }
    single<NetworkFeature> { NetworkFeatureImpl(interactor = get()) }
}
