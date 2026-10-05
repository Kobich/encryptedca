// The profile's SSLContext against a local TLS server that requires a client certificate.
// Test certificates come from tools/test-certs/make_test_certs.sh; the .p12 password is "test".
package com.engboost.encryptedca.core.network.impl.data

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import kotlin.concurrent.thread

class MutualTlsTest {
    private val profileContext = SslContextRepositoryImpl().create(clientCredentials())

    @Test
    fun handshakeSucceedsWithServerOfTheProfileCa() {
        assertTrue(connects(server("server.p12", trusting = "ca.pem")))
    }

    @Test
    fun serverSignedByAnotherCaIsRejected() {
        assertFalse(connects(server("other-server.p12", trusting = "ca.pem")))
    }

    @Test
    fun serverExpectingAnotherCaDoesNotGetTheClientKey() {
        assertFalse(connects(server("server.p12", trusting = "other-ca.pem")))
    }

    private fun connects(serverContext: SSLContext): Boolean {
        val loopback = InetAddress.getLoopbackAddress()
        val serverSocket = serverContext.serverSocketFactory.createServerSocket(0, 1, loopback) as SSLServerSocket
        serverSocket.use {
            serverSocket.needClientAuth = true
            serverSocket.soTimeout = TIMEOUT_MS
            val server = thread {
                runCatching {
                    (serverSocket.accept() as SSLSocket).use { socket ->
                        socket.soTimeout = TIMEOUT_MS
                        socket.startHandshake()
                        socket.outputStream.write(1)
                        socket.outputStream.flush()
                    }
                }
            }
            // TLS 1.3 reports a rejected client certificate only after the handshake, so a byte is read too.
            val connected = runCatching {
                (profileContext.socketFactory.createSocket(loopback, serverSocket.localPort) as SSLSocket).use { socket ->
                    socket.soTimeout = TIMEOUT_MS
                    socket.startHandshake()
                    socket.inputStream.read() == 1
                }
            }.getOrDefault(false)
            server.join(TIMEOUT_MS.toLong())
            return connected
        }
    }

    private fun server(p12: String, trusting: String): SSLContext {
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(pkcs12(p12), PASSWORD) }
            .keyManagers
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry("ca", certificate(trusting))
        }
        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(trustStore) }
            .trustManagers
        return SSLContext.getInstance("TLS").apply { init(keyManagers, trustManagers, null) }
    }

    private fun clientCredentials(): ClientCredentials {
        val keyStore = pkcs12("client.p12")
        val alias = keyStore.aliases().toList().first(keyStore::isKeyEntry)
        return ClientCredentials(
            profileId = "test",
            privateKey = keyStore.getKey(alias, PASSWORD) as PrivateKey,
            certificateChain = keyStore.getCertificateChain(alias).map { it as X509Certificate },
            trustAnchor = certificate("ca.pem"),
        )
    }

    private fun pkcs12(name: String) = KeyStore.getInstance("PKCS12").apply { resource(name).use { load(it, PASSWORD) } }

    private fun certificate(name: String) =
        resource(name).use { CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate }

    private fun resource(name: String) = checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "Missing test resource $name" }

    private companion object {
        val PASSWORD = "test".toCharArray()
        const val TIMEOUT_MS = 5_000
    }
}
