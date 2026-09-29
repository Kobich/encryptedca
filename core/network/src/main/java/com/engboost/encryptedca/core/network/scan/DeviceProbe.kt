// Проверяет один хост: TCP-подключение через Wi-Fi сеть, затем mTLS-рукопожатие с активным профилем.
// Отказ в подключении значит, что хост жив. При таймауте живость проверяется ping'ом.
// Ping — отдельный процесс, его нельзя привязать к Wi-Fi, поэтому он идёт по маршруту по умолчанию и может
// пропустить хост. На доверие к устройству ping не влияет никогда.
// Устройства адресуются по IP, поэтому проверяется только цепочка до CA профиля, без имени хоста.
// Таймаут рукопожатия большой: RSA-4096 на слабом устройстве может считаться несколько секунд.
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

internal class DeviceProbe(
    private val network: Network,
    private val sslContext: SSLContext,
    private val port: Int,
) {
    fun check(host: Inet4Address): FoundDevice? {
        val socket = network.socketFactory.createSocket()
        try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        } catch (e: IOException) {
            socket.close()
            val alive = e is ConnectException || ping(host)
            return if (alive) FoundDevice(host, DeviceStatus.PORT_UNREACHABLE) else null
        }
        val fingerprint = handshake(socket, host)
        val status = if (fingerprint != null) DeviceStatus.TRUSTED else DeviceStatus.HANDSHAKE_FAILED
        return FoundDevice(host, status, fingerprint)
    }

    private fun handshake(socket: Socket, host: Inet4Address): String? =
        try {
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
        const val HANDSHAKE_TIMEOUT_MS = 10_000
        const val PING_TIMEOUT_S = 2L
        const val TAG = "DeviceProbe"
    }
}
