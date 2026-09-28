package com.engboost.encryptedca.core.certificates

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import com.engboost.encryptedca.core.certificates.CertificateProfileError.STORAGE_FAILED
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.X509ExtendedKeyManager

internal class ClientKeyMaterial(
    val privateKey: PrivateKey,
    val certificateChain: Array<X509Certificate>,
)

/** Client keys in Android Keystore under `mtls_client_<profileId>`; the alias format must stay stable. */
internal class ClientKeyStorage {

    fun save(profileId: String, material: ClientKeyMaterial) = rethrowAs(STORAGE_FAILED, "Could not save client key") {
        val rsa = material.privateKey.algorithm.equals(KeyProperties.KEY_ALGORITHM_RSA, ignoreCase = true)
        // For RSA-PSS (TLS 1.3) Conscrypt pads the data itself and asks the key for a raw RSA operation,
        // which Keystore treats as decryption without padding; the key must allow it or the handshake fails.
        val purposes = if (rsa) KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT else KeyProperties.PURPOSE_SIGN
        val protection = KeyProtection.Builder(purposes)
            .setDigests(
                KeyProperties.DIGEST_NONE,
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512,
            )
            .apply {
                if (rsa) {
                    setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                    )
                    setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE,
                        KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1,
                    )
                }
            }
            .build()
        androidKeyStore().setEntry(
            alias(profileId),
            KeyStore.PrivateKeyEntry(material.privateKey, material.certificateChain),
            protection,
        )
    }

    fun exists(profileId: String): Boolean = rethrowAs(STORAGE_FAILED, "Could not inspect client key") {
        androidKeyStore().isKeyEntry(alias(profileId))
    }

    fun delete(profileId: String) = rethrowAs(STORAGE_FAILED, "Could not delete client key") {
        val keyStore = androidKeyStore()
        if (keyStore.containsAlias(alias(profileId))) {
            keyStore.deleteEntry(alias(profileId))
        }
    }

    fun entry(profileId: String): KeyStore.PrivateKeyEntry = rethrowAs(STORAGE_FAILED, "Could not open client key") {
        androidKeyStore().getEntry(alias(profileId), null) as? KeyStore.PrivateKeyEntry
            ?: throw CertificateProfileException(CertificateProfileError.PROFILE_INCOMPLETE, "Client key is missing")
    }

    fun keyManager(profileId: String): X509ExtendedKeyManager = rethrowAs(STORAGE_FAILED, "Could not open client key") {
        val delegate = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(androidKeyStore(), null) }
            .keyManagers
            .filterIsInstance<X509ExtendedKeyManager>()
            .firstOrNull()
            ?: throw CertificateProfileException(STORAGE_FAILED, "No X509ExtendedKeyManager was created")
        SelectedAliasKeyManager(delegate, alias(profileId))
    }

    private fun alias(profileId: String) = "mtls_client_$profileId"
}

internal const val ANDROID_KEY_STORE = "AndroidKeyStore"

internal fun androidKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
