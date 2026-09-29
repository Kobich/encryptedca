// Читает .p12 и CA из байтов и проверяет, что сертификаты сейчас действительны.
// Контейнер .p12 без пароля Android открывает только с паролем из одного символа NUL.
// CA выбирает сам пользователь, поэтому принимается и старый самоподписанный CA без BasicConstraints CA:TRUE.
// Из .p12 берётся первый в порядке контейнера ключ с цепочкой, остальные записи не открываются.
package com.engboost.encryptedca.core.certificates.parsing

import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.CERTIFICATE_INVALID
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.PKCS12_KEY_UNAVAILABLE
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.PrivateKeyWithChain
import com.engboost.encryptedca.core.certificates.model.rethrowAs
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

internal object CertificateParser {
    private const val KEY_UNAVAILABLE = "PKCS#12 opened, but the private key is unavailable"

    fun readPkcs12(encoded: ByteArray, password: CharArray): PrivateKeyWithChain {
        val effectivePassword = if (password.isEmpty()) charArrayOf('\u0000') else password
        val container = try {
            KeyStore.getInstance("PKCS12").apply { load(encoded.inputStream(), effectivePassword) }
        } catch (e: Exception) {
            throw CertificateProfileException(PKCS12_PASSWORD_OR_CORRUPT, "PKCS#12 could not be opened", e)
        }
        val entry = rethrowAs(PKCS12_KEY_UNAVAILABLE, KEY_UNAVAILABLE) { firstPrivateKeyEntry(container, effectivePassword) }
            ?: throw CertificateProfileException(PKCS12_KEY_UNAVAILABLE, KEY_UNAVAILABLE)

        val chain = entry.certificateChain.map { it as? X509Certificate ?: throw notX509() }
        if (chain.isEmpty()) throw notX509()
        chain.first().requireCurrentlyValid()
        return PrivateKeyWithChain(entry.privateKey, chain.toTypedArray())
    }

    fun readCaPem(encoded: ByteArray): X509Certificate {
        val certificate = rethrowAs(CERTIFICATE_INVALID, "CA PEM could not be parsed") {
            CertificateFactory.getInstance("X.509").generateCertificate(encoded.inputStream())
        }
        return (certificate as? X509Certificate ?: throw notX509()).also { it.requireCurrentlyValid() }
    }

    private fun firstPrivateKeyEntry(container: KeyStore, password: CharArray): KeyStore.PrivateKeyEntry? {
        for (alias in container.aliases()) {
            if (!container.isKeyEntry(alias)) continue
            val entry = container.getEntry(alias, KeyStore.PasswordProtection(password))
            if (entry is KeyStore.PrivateKeyEntry) return entry
        }
        return null
    }

    private fun notX509() = CertificateProfileException(CERTIFICATE_INVALID, "Certificate is not X.509")
}
