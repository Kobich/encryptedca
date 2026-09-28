package com.engboost.encryptedca.core.certificates.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.PROFILE_INCOMPLETE
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.rethrowAs
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the CA DER as `[IV length][IV][AES-GCM ciphertext]` in `noBackupFilesDir`.
 * The format and key alias must stay compatible with profiles already on devices.
 */
internal class CaCertificateStore(context: Context) {
    private val directory = context.noBackupFilesDir

    fun write(profileId: String, caCertificate: X509Certificate) {
        val temp = tempFile(profileId)
        try {
            rethrowAs(STORAGE_FAILED, "Could not save encrypted CA") {
                val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
                val iv = cipher.iv
                if (iv == null || iv.size !in IV_SIZES) throw IOException("Invalid GCM IV from Android Keystore")
                val encrypted = cipher.doFinal(caCertificate.encoded)
                FileOutputStream(temp).use { output ->
                    output.write(iv.size)
                    output.write(iv)
                    output.write(encrypted)
                    output.fd.sync()
                }
                if (!temp.renameTo(caFile(profileId))) throw IOException("Could not move encrypted CA into place")
            }
        } catch (e: CertificateProfileException) {
            if (temp.exists() && !temp.delete()) e.addSuppressed(IOException("Could not delete ${temp.name}"))
            throw e
        }
    }

    fun read(profileId: String): X509Certificate = rethrowAs(STORAGE_FAILED, "Could not read encrypted CA") {
        val bytes = caFile(profileId).readBytes()
        val ivLength = bytes.firstOrNull()?.toInt() ?: throw IOException("Encrypted CA is empty")
        val dataOffset = 1 + ivLength
        if (ivLength !in IV_SIZES || bytes.size <= dataOffset) throw IOException("Invalid encrypted CA header")
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, existingKey(), GCMParameterSpec(TAG_BITS, bytes, 1, ivLength))
        }
        val der = cipher.doFinal(bytes, dataOffset, bytes.size - dataOffset)
        CertificateFactory.getInstance("X.509").generateCertificate(der.inputStream()) as X509Certificate
    }

    fun exists(profileId: String): Boolean = caFile(profileId).isFile

    fun delete(profileId: String) {
        val undeleted = listOf(caFile(profileId), tempFile(profileId)).filter { it.exists() && !it.delete() }
        if (undeleted.isNotEmpty()) {
            throw CertificateProfileException(STORAGE_FAILED, "Could not delete ${undeleted.joinToString { it.name }}")
        }
    }

    private fun existingKey(): SecretKey = openAndroidKeyStore().getKey(KEY_ALIAS, null) as? SecretKey
        ?: throw CertificateProfileException(PROFILE_INCOMPLETE, "CA encryption key is missing")

    private fun getOrCreateKey(): SecretKey =
        openAndroidKeyStore().getKey(KEY_ALIAS, null) as? SecretKey
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
                init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }

    private fun caFile(profileId: String) = File(directory, "$profileId.ca.enc")

    private fun tempFile(profileId: String) = File(directory, "$profileId.ca.enc.tmp")

    private companion object {
        const val KEY_ALIAS = "mtls_ca_storage_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        val IV_SIZES = 12..16
    }
}
