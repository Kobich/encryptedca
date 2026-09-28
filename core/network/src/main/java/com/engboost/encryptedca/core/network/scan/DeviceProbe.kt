package com.engboost.encryptedca.core.network.scan

import android.net.Network
import android.util.Log
import com.engboost.encryptedca.core.network.tls.sha256Fingerprint
import java.io.IOException
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * Checks one host: a TCP connection through the Wi-Fi [network], then the mTLS handshake.
 *
 * When the connection times out, ping decides whether the host is up at all. Ping is a separate process
 * and can't be bound to [network], so it follows the phone's default route; if that isn't the Wi-Fi,
 * a live host may be missed. That only hides a host whose port couldn't be reached anyway: ping never
 * affects whether a device counts as trusted.
 */
internal class DeviceProbe(
    private val network: Network,
    private val sslContext: SSLContext,
    private val port: Int,
) {
    /** `null` when the host doesn't answer at all. */
    fun check(host: Inet4Address): FoundDevice? {
        val socket = network.socketFactory.createSocket()
        try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        } catch (e: IOException) {
            socket.close()
            // A refused connection proves the host is up; a timeout may be a firewall, so ask ping.
            val alive = e is ConnectException || ping(host)
            return if (alive) FoundDevice(host, DeviceStatus.PORT_UNREACHABLE) else null
        }
        val fingerprint = handshake(socket, host)
        val status = if (fingerprint != null) DeviceStatus.TRUSTED else DeviceStatus.HANDSHAKE_FAILED
        return FoundDevice(host, status, fingerprint)
    }

    /** The server certificate's fingerprint, or `null` when the handshake failed. Closes [socket]. */
    private fun handshake(socket: Socket, host: Inet4Address): String? =
        try {
            // Devices are addressed by IP, so only the chain to the profile's CA is checked, not a host name.
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
        const val CONNECT_TIMEOUT_MS = 1_000
        // RSA-4096 on an embedded server can take several seconds.
        const val HANDSHAKE_TIMEOUT_MS = 10_000
        const val PING_TIMEOUT_S = 2L
        const val TAG = "DeviceProbe"
    }
}
