package com.engboost.encryptedca.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

class DeviceScanner(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    /** The current Wi-Fi network, or `null` while there is none. */
    fun wifi(): Flow<LocalNetwork?> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
                trySend(properties.toLocalNetwork(network))
            }

            override fun onLost(network: Network) {
                trySend(null)
            }
        }
        // A network of devices usually has no internet; the default request would skip it.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        trySend(null)
        connectivity.registerNetworkCallback(request, callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChangedBy { it?.toString() }

    /** Emits each live host as soon as it has been checked; hosts that don't answer are skipped. */
    fun scan(wifi: LocalNetwork, sslContext: SSLContext, port: Int = HTTPS_PORT): Flow<FoundDevice> = channelFlow {
        val permits = Semaphore(PARALLEL_PROBES)
        wifi.hosts().forEach { host ->
            launch {
                permits.withPermit { probe(wifi.network, sslContext, host, port)?.let { send(it) } }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun probe(network: Network, sslContext: SSLContext, host: Inet4Address, port: Int): FoundDevice? {
        val socket = network.socketFactory.createSocket()
        try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        } catch (e: IOException) {
            socket.close()
            // A refused connection proves the host is up; a timeout may be a firewall, so ask ping.
            val alive = e is ConnectException || ping(host)
            return if (alive) FoundDevice(host, DeviceStatus.NO_TLS) else null
        }
        // Devices are addressed by IP, so only the chain to the profile's CA is checked, not a host name.
        val fingerprint = try {
            val tls = sslContext.socketFactory.createSocket(socket, host.hostAddress, port, true) as SSLSocket
            tls.use {
                it.soTimeout = HANDSHAKE_TIMEOUT_MS
                it.startHandshake()
                (it.session.peerCertificates.first() as X509Certificate).sha256Fingerprint()
            }
        } catch (e: IOException) {
            Log.i(TAG, "TLS check failed for ${host.hostAddress}", e)
            null
        } finally {
            socket.close()
        }
        val status = if (fingerprint != null) DeviceStatus.TRUSTED else DeviceStatus.UNTRUSTED
        return FoundDevice(host, status, fingerprint)
    }

    private fun ping(host: Inet4Address): Boolean {
        val process = try {
            ProcessBuilder("ping", "-c", "1", "-W", "1", host.hostAddress).redirectErrorStream(true).start()
        } catch (e: IOException) {
            return false
        }
        return try {
            process.waitFor(PING_TIMEOUT_S, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (e: InterruptedException) {
            false
        } finally {
            process.destroy()
        }
    }

    private fun LinkProperties.toLocalNetwork(network: Network): LocalNetwork? =
        linkAddresses.firstOrNull { it.address is Inet4Address }
            ?.let { LocalNetwork(network, it.address as Inet4Address, it.prefixLength) }

    private companion object {
        const val HTTPS_PORT = 443
        const val PARALLEL_PROBES = 64
        const val CONNECT_TIMEOUT_MS = 1_000
        // RSA-4096 on an embedded server can take several seconds.
        const val HANDSHAKE_TIMEOUT_MS = 10_000
        const val PING_TIMEOUT_S = 2L
        const val TAG = "DeviceScanner"
    }
}
