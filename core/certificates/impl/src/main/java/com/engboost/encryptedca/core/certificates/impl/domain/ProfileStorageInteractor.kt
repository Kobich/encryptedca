// A profile is three records that change together: the client key, the encrypted CA
// and an entry in the profile list.
// Calls run one at a time under a mutex, so nobody sees a half-written profile.
// Import and delete can't be cancelled halfway: they either finish or roll back step by step.
package com.engboost.encryptedca.core.certificates.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.PROFILE_INCOMPLETE
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.core.certificates.impl.domain.parsing.CertificateParser
import com.engboost.encryptedca.core.certificates.impl.domain.parsing.PrivateKeyWithChain
import com.engboost.encryptedca.core.certificates.impl.domain.parsing.requireCurrentlyValid
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

internal class ProfileStorageInteractor(
    private val keys: ClientKeyRepository,
    private val caCertificates: CaCertificateRepository,
    private val profiles: ProfileIndexRepository,
) {
    private val mutex = Mutex()
    private val _index = MutableStateFlow<ProfileIndex?>(null)

    val index: StateFlow<ProfileIndex?> = _index.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { publishIndex() }
    }

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

    suspend fun selectProfile(profileId: String) = changeProfiles {
        loadCredentials(profileId)
        profiles.setActive(profileId)
    }

    suspend fun deleteProfile(profileId: String) = changeProfiles {
        if (profiles.contains(profileId)) removeProfile(profileId)
    }

    suspend fun loadActiveCredentials(): ClientCredentials? = withContext(Dispatchers.IO) {
        mutex.withLock { profiles.activeProfileId?.let(::loadCredentials) }
    }

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
