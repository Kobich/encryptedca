// Checks one host on port 443: a TCP connection over the Wi-Fi network, then an mTLS handshake with the active profile.
// A refused connection means the host is up. After a timeout, ping decides whether it is up.
// Ping is a separate process and can't be bound to the Wi-Fi, so it follows the default route and may
// miss a host. Ping never affects whether a device is trusted.
// Devices are addressed by IP, so only the chain to the profile's CA is checked, not a host name.
// The handshake timeout is long: RSA-4096 on a slow device can take several seconds.
package com.engboost.encryptedca.core.network.impl.data

import android.net.Network
import android.util.Log
import com.engboost.encryptedca.core.network.api.entity.DeviceStatus
import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.core.network.api.sha256Fingerprint
import com.engboost.encryptedca.core.network.impl.domain.DeviceRepository
import java.io.IOException
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

internal class DeviceRepositoryImpl : DeviceRepository {

    // The socket is closed on every path, including unexpected exceptions.
    // The calls here block, and cancelling the coroutine doesn't interrupt them; they end by timeout.
    override fun probe(networkHandle: Long, sslContext: SSLContext, host: Inet4Address): FoundDevice? {
        val socket = Network.fromNetworkHandle(networkHandle).socketFactory.createSocket()
        return socket.use {
            try {
                socket.connect(InetSocketAddress(host, PORT), CONNECT_TIMEOUT_MS)
            } catch (e: IOException) {
                val alive = e is ConnectException || ping(host)
                return if (alive) FoundDevice(host, DeviceStatus.PORT_UNREACHABLE) else null
            }
            val fingerprint = handshake(sslContext, socket, host)
            val status = if (fingerprint != null) DeviceStatus.TRUSTED else DeviceStatus.HANDSHAKE_FAILED
            FoundDevice(host, status, fingerprint)
        }
    }

    private fun handshake(sslContext: SSLContext, socket: Socket, host: Inet4Address): String? =
        try {
            val tls = sslContext.socketFactory.createSocket(socket, host.hostAddress, PORT, true) as SSLSocket
            tls.use {
                it.soTimeout = HANDSHAKE_TIMEOUT_MS
                it.startHandshake()
                (it.session.peerCertificates.first() as X509Certificate).sha256Fingerprint()
            }
        } catch (e: IOException) {
            Log.i(TAG, "TLS check failed for ${host.hostAddress}", e)
            null
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

    private companion object {
        const val PORT = 443
        const val CONNECT_TIMEOUT_MS = 1_000
        const val HANDSHAKE_TIMEOUT_MS = 10_000
        const val PING_TIMEOUT_S = 2L
        const val TAG = "DeviceRepository"
    }
}
