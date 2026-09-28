package com.engboost.encryptedca.core.certificates

import com.engboost.encryptedca.core.certificates.CertificateProfileError.CERTIFICATE_INVALID
import com.engboost.encryptedca.core.certificates.CertificateProfileError.PKCS12_KEY_UNAVAILABLE
import com.engboost.encryptedca.core.certificates.CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

internal class CertificateMaterialReader {

    fun readPkcs12(encoded: ByteArray, password: CharArray): ClientKeyMaterial {
        // Android's PKCS#12 provider opens passwordless containers only with a single NUL character.
        val effectivePassword = if (password.isEmpty()) charArrayOf('\u0000') else password
        val container = try {
            KeyStore.getInstance("PKCS12").apply { load(encoded.inputStream(), effectivePassword) }
        } catch (e: Exception) {
            throw CertificateProfileException(PKCS12_PASSWORD_OR_CORRUPT, "PKCS#12 could not be opened", e)
        }
        val entry = rethrowAs(PKCS12_KEY_UNAVAILABLE, KEY_UNAVAILABLE) {
            container.aliases().toList()
                .filter(container::isKeyEntry)
                .map { container.getEntry(it, KeyStore.PasswordProtection(effectivePassword)) }
                .filterIsInstance<KeyStore.PrivateKeyEntry>()
                .firstOrNull()
        } ?: throw CertificateProfileException(PKCS12_KEY_UNAVAILABLE, KEY_UNAVAILABLE)
        return toClientMaterial(entry)
    }

    /**
     * The CA is an explicit trust anchor chosen by the user, so a legacy self-signed issuer
     * without `BasicConstraints CA:TRUE` is accepted.
     */
    fun readCaPem(encoded: ByteArray): X509Certificate {
        val certificate = rethrowAs(CERTIFICATE_INVALID, "CA PEM could not be parsed") {
            CertificateFactory.getInstance("X.509").generateCertificate(encoded.inputStream())
        }
        return (certificate as? X509Certificate ?: throw unsuitable()).also { it.requireCurrentlyValid() }
    }

    private fun toClientMaterial(entry: KeyStore.PrivateKeyEntry): ClientKeyMaterial {
        val chain = entry.certificateChain.map { it as? X509Certificate ?: throw unsuitable() }
        if (chain.isEmpty()) throw unsuitable()
        chain.first().requireCurrentlyValid()
        return ClientKeyMaterial(entry.privateKey, chain.toTypedArray())
    }

    private fun unsuitable() = CertificateProfileException(CERTIFICATE_INVALID, "Certificate is not X.509")

    private companion object {
        const val KEY_UNAVAILABLE = "PKCS#12 opened, but the private key is unavailable"
    }
}

internal fun X509Certificate.requireCurrentlyValid() =
    rethrowAs(CERTIFICATE_INVALID, "Certificate is expired or not yet valid") { checkValidity() }
