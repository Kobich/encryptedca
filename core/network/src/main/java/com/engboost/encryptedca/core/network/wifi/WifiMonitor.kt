package com.engboost.encryptedca.core.network.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.Inet4Address

class WifiMonitor(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    /**
     * The current Wi-Fi network, or `null` while there is none. A reconnect emits a new value even
     * when the address and prefix stay the same, because sockets must be bound to the new [Network].
     */
    fun observeNetwork(): Flow<LocalNetwork?> = callbackFlow {
        var current: LocalNetwork? = null

        fun publish(network: LocalNetwork?) {
            current = network
            trySend(network)
        }

        // Callbacks arrive one at a time on the ConnectivityManager thread.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
                val local = properties.toLocalNetwork(network)
                if (local != null || current?.network == network) publish(local)
            }

            // The previous network may be reported lost after the next one is already up.
            override fun onLost(network: Network) {
                if (current?.network == network) publish(null)
            }
        }
        // A network of devices usually has no internet; the default request would skip it.
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
            ?.let { LocalNetwork(network, it.address as Inet4Address, it.prefixLength) }
}
