// Watches the current Wi-Fi network; null means there is none.
// A network without internet counts too: a network of devices usually has none.
// A reconnect emits a new value even with the same address, because sockets must bind to the new network.
// The old network may be reported lost after the new one is up, so a loss counts only for the current one.
package com.engboost.encryptedca.core.network.impl.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.core.network.impl.domain.WifiRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.Inet4Address

internal class WifiRepositoryImpl(context: Context) : WifiRepository {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override fun observeNetwork(): Flow<LocalNetwork?> = callbackFlow {
        var current: LocalNetwork? = null

        fun publish(network: LocalNetwork?) {
            current = network
            trySend(network)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
                val local = properties.toLocalNetwork(network)
                if (local != null || current?.networkHandle == network.networkHandle) publish(local)
            }

            override fun onLost(network: Network) {
                if (current?.networkHandle == network.networkHandle) publish(null)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        publish(null)
        connectivity.registerNetworkCallback(request, callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun LinkProperties.toLocalNetwork(network: Network): LocalNetwork? =
        linkAddresses.firstOrNull { it.address is Inet4Address }
            ?.let { LocalNetwork(network.networkHandle, it.address as Inet4Address, it.prefixLength) }
}
