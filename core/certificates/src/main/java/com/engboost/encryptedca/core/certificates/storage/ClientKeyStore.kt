package com.engboost.encryptedca.core.certificates.storage

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.PROFILE_INCOMPLETE
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.PrivateKeyWithChain
import com.engboost.encryptedca.core.certificates.model.rethrowAs
import java.security.KeyStore

/** Client keys in Android Keystore under `mtls_client_<profileId>`; the alias format must stay stable. */
internal class ClientKeyStore {

    fun save(profileId: String, key: PrivateKeyWithChain) = rethrowAs(STORAGE_FAILED, "Could not save client key") {
        val rsa = key.privateKey.algorithm.equals(KeyProperties.KEY_ALGORITHM_RSA, ignoreCase = true)
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
        openAndroidKeyStore().setEntry(
            alias(profileId),
            KeyStore.PrivateKeyEntry(key.privateKey, key.certificateChain),
            protection,
        )
    }

    fun read(profileId: String): KeyStore.PrivateKeyEntry = rethrowAs(STORAGE_FAILED, "Could not open client key") {
        openAndroidKeyStore().getEntry(alias(profileId), null) as? KeyStore.PrivateKeyEntry
            ?: throw CertificateProfileException(PROFILE_INCOMPLETE, "Client key is missing")
    }

    fun exists(profileId: String): Boolean = rethrowAs(STORAGE_FAILED, "Could not inspect client key") {
        openAndroidKeyStore().isKeyEntry(alias(profileId))
    }

    fun delete(profileId: String) = rethrowAs(STORAGE_FAILED, "Could not delete client key") {
        val keyStore = openAndroidKeyStore()
        if (keyStore.containsAlias(alias(profileId))) {
            keyStore.deleteEntry(alias(profileId))
        }
    }

    private fun alias(profileId: String) = "mtls_client_$profileId"
}
