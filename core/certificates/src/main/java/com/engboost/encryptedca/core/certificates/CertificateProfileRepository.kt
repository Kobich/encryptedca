package com.engboost.encryptedca.core.certificates

import android.content.Context
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.PROFILE_INCOMPLETE
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.ClientCredentials
import com.engboost.encryptedca.core.certificates.model.PrivateKeyWithChain
import com.engboost.encryptedca.core.certificates.model.ProfileIndex
import com.engboost.encryptedca.core.certificates.model.asProfileException
import com.engboost.encryptedca.core.certificates.parsing.CertificateParser
import com.engboost.encryptedca.core.certificates.parsing.requireCurrentlyValid
import com.engboost.encryptedca.core.certificates.storage.CaCertificateStore
import com.engboost.encryptedca.core.certificates.storage.ClientKeyStore
import com.engboost.encryptedca.core.certificates.storage.ProfileIndexStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import java.util.UUID

/**
 * The only entry point to certificate profiles; one instance per app.
 *
 * A profile is three records kept in sync: the client key ([ClientKeyStore]), the encrypted CA
 * ([CaCertificateStore]) and an entry in the index ([ProfileIndexStore]).
 *
 * Every call holds [mutex], so reads never see a half-written profile. Changes also run
 * [NonCancellable]: once started, an import or delete finishes or rolls back even if the caller leaves.
 */
class CertificateProfileRepository(context: Context) {
    private val keys = ClientKeyStore()
    private val caCertificates = CaCertificateStore(context.applicationContext)
    private val profiles = ProfileIndexStore(context.applicationContext)

    private val mutex = Mutex()
    private val _index = MutableStateFlow<ProfileIndex?>(null)

    /** `null` until the first [refresh] or change. */
    val index: StateFlow<ProfileIndex?> = _index.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { publishIndex() }
    }

    /** Takes ownership of [p12] and [password] and wipes them whatever the outcome. Returns the new profile id. */
    suspend fun importProfile(displayName: String?, p12: ByteArray, password: CharArray, caPem: ByteArray): String =
        changeProfiles {
            try {
                val clientKey = CertificateParser.readPkcs12(p12, password)
                val ca = CertificateParser.readCaPem(caPem)
                persistProfile(displayName, clientKey, ca)
            } finally {
                password.wipe()
                p12.fill(0)
            }
        }

    /** Rejects a profile whose key or CA is missing or whose CA has expired. */
    suspend fun selectProfile(profileId: String) = changeProfiles {
        loadCredentials(profileId)
        profiles.setActive(profileId)
    }

    /** Deleting the active profile clears the selection. */
    suspend fun deleteProfile(profileId: String) = changeProfiles {
        if (profiles.contains(profileId)) removeProfile(profileId)
    }

    /** Credentials of the selected profile, or `null` when none is selected. */
    suspend fun loadActiveCredentials(): ClientCredentials? = withContext(Dispatchers.IO) {
        mutex.withLock { profiles.activeProfileId?.let(::loadCredentials) }
    }

    /** Keystore, files and preferences can't share a transaction, so a failure is undone step by step. */
    private fun persistProfile(displayName: String?, clientKey: PrivateKeyWithChain, ca: X509Certificate): String {
        val profileId = UUID.randomUUID().toString()
        val rollback = ArrayDeque<() -> Unit>()
        try {
            rollback.addFirst { keys.delete(profileId) }
            keys.save(profileId, clientKey)

            rollback.addFirst { caCertificates.delete(profileId) }
            caCertificates.write(profileId, ca)

            rollback.addFirst { profiles.unregister(profileId) }
            profiles.register(profileId, displayName)
            return profileId
        } catch (e: Exception) {
            val failure = e.asProfileException(STORAGE_FAILED, "Could not import certificate profile")
            for (step in rollback) {
                runCatching(step).exceptionOrNull()?.let(failure::addSuppressed)
            }
            throw failure
        }
    }

    /** Tries to delete both the key and the CA; the index entry goes only when both are gone. */
    private fun removeProfile(profileId: String) {
        val keyFailure = runCatching { keys.delete(profileId) }.exceptionOrNull()
        val caFailure = runCatching { caCertificates.delete(profileId) }.exceptionOrNull()
        if (keyFailure != null) {
            caFailure?.let(keyFailure::addSuppressed)
            throw keyFailure
        }
        if (caFailure != null) throw caFailure
        profiles.unregister(profileId)
    }

    private fun loadCredentials(profileId: String): ClientCredentials {
        if (!profiles.contains(profileId)) {
            throw CertificateProfileException(PROFILE_INCOMPLETE, "Unknown profileId")
        }
        if (!keys.exists(profileId) || !caCertificates.exists(profileId)) {
            throw CertificateProfileException(PROFILE_INCOMPLETE, "Certificate profile is incomplete")
        }
        val ca = caCertificates.read(profileId)
        ca.requireCurrentlyValid()
        val key = keys.read(profileId)
        return ClientCredentials(
            profileId = profileId,
            privateKey = key.privateKey,
            certificateChain = key.certificateChain.filterIsInstance<X509Certificate>(),
            trustAnchor = ca,
        )
    }

    /**
     * Runs [change] under the lock without cancellation, then publishes the index whether or not
     * [change] succeeded. The change's own error wins; a failed publish is attached to it.
     */
    private suspend fun <T> changeProfiles(change: () -> T): T = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            val result = try {
                change()
            } catch (e: Exception) {
                runCatching { publishIndex() }.exceptionOrNull()?.let(e::addSuppressed)
                throw e
            }
            publishIndex()
            result
        }
    }

    private fun publishIndex() {
        _index.value = profiles.read()
    }
}
