package com.engboost.encryptedca.core.network.impl.di

import com.engboost.encryptedca.core.network.api.scan.DeviceScanner
import com.engboost.encryptedca.core.network.api.tls.SslContextFactory
import com.engboost.encryptedca.core.network.api.wifi.WifiMonitor
import com.engboost.encryptedca.core.network.impl.scan.DefaultDeviceScanner
import com.engboost.encryptedca.core.network.impl.tls.DefaultSslContextFactory
import com.engboost.encryptedca.core.network.impl.wifi.AndroidWifiMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreNetworkModule = module {
    factory<WifiMonitor> { AndroidWifiMonitor(androidContext()) }
    factory<DeviceScanner> { DefaultDeviceScanner() }
    factory<SslContextFactory> { DefaultSslContextFactory() }
}
