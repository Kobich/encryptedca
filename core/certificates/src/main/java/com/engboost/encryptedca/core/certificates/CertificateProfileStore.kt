package com.engboost.encryptedca.core.certificates

import android.content.Context
import com.engboost.encryptedca.core.certificates.CertificateProfileError.PROFILE_INCOMPLETE
import com.engboost.encryptedca.core.certificates.CertificateProfileError.STORAGE_FAILED
import java.security.cert.X509Certificate
import java.util.UUID

/** Coordinates the key, CA and index storages. Blocking; [CertificateProfileRepository] runs it off the main thread. */
internal class CertificateProfileStore(
    private val reader: CertificateMaterialReader,
    private val keys: ClientKeyStorage,
    private val caStorage: EncryptedCaStorage,
    private val index: ProfileIndexStore,
) {
    constructor(context: Context) : this(
        CertificateMaterialReader(),
        ClientKeyStorage(),
        EncryptedCaStorage(context),
        ProfileIndexStore(context),
    )

    fun readIndex(): ProfileIndex = index.read()

    /** Wipes [p12] and [password] whatever the outcome. */
    fun importProfile(displayName: String?, p12: ByteArray, password: CharArray, caPem: ByteArray): String {
        try {
            val client = reader.readPkcs12(p12, password)
            val ca = reader.readCaPem(caPem)
            val profileId = UUID.randomUUID().toString()
            // Keystore, files and preferences can't share a transaction, so a failure is undone step by step.
            val rollback = ArrayDeque<() -> Unit>()
            try {
                rollback.addFirst { keys.delete(profileId) }
                keys.save(profileId, client)
                rollback.addFirst { caStorage.delete(profileId) }
                caStorage.write(profileId, ca)
                rollback.addFirst { index.unregister(profileId) }
                index.register(profileId, displayName)
                return profileId
            } catch (e: Exception) {
                val failure = e.asProfileException(STORAGE_FAILED, "Could not import certificate profile")
                rollback.forEach { step -> runCatching(step).exceptionOrNull()?.let(failure::addSuppressed) }
                throw failure
            }
        } finally {
            password.wipe()
            p12.fill(0)
        }
    }

    /** Rejects a profile whose key or CA is missing or whose CA has expired. */
    fun setActiveProfile(profileId: String) {
        credentials(profileId)
        index.setActive(profileId)
    }

    fun activeCredentials(): ClientCredentials? = index.activeProfileId?.let(this::credentials)

    /** Removes the key, the CA file and the index entry; deleting the active profile clears the selection. */
    fun deleteProfile(profileId: String) {
        if (!index.contains(profileId)) return
        val failures = listOf({ keys.delete(profileId) }, { caStorage.delete(profileId) })
            .mapNotNull { runCatching(it).exceptionOrNull() }
        if (failures.isNotEmpty()) {
            throw failures.first().apply { failures.drop(1).forEach(this::addSuppressed) }
        }
        index.unregister(profileId)
    }

    private fun credentials(profileId: String): ClientCredentials {
        if (!index.contains(profileId)) {
            throw CertificateProfileException(PROFILE_INCOMPLETE, "Unknown profileId")
        }
        if (!keys.exists(profileId) || !caStorage.exists(profileId)) {
            throw CertificateProfileException(PROFILE_INCOMPLETE, "Certificate profile is incomplete")
        }
        val ca = caStorage.read(profileId).also { it.requireCurrentlyValid() }
        val entry = keys.entry(profileId)
        return ClientCredentials(
            profileId = profileId,
            keyManager = keys.keyManager(profileId),
            privateKey = entry.privateKey,
            certificateChain = entry.certificateChain.filterIsInstance<X509Certificate>(),
            trustAnchor = ca,
        )
    }
}
