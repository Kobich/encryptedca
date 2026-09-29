// Следит за текущей Wi-Fi сетью. null — Wi-Fi нет.
// Сеть без интернета тоже подходит: сеть с устройствами обычно без него.
// Переподключение даёт новое значение, даже если адрес не изменился: сокеты нужно привязать к новой сети.
// Потеря старой сети может прийти уже после появления новой, поэтому она учитывается только для текущей.
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

    fun observeNetwork(): Flow<LocalNetwork?> = callbackFlow {
        var current: LocalNetwork? = null

        fun publish(network: LocalNetwork?) {
            current = network
            trySend(network)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
                val local = properties.toLocalNetwork(network)
                if (local != null || current?.network == network) publish(local)
            }

            override fun onLost(network: Network) {
                if (current?.network == network) publish(null)
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
            ?.let { LocalNetwork(network, it.address as Inet4Address, it.prefixLength) }
}
