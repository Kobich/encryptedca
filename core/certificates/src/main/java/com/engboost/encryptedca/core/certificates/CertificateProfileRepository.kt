package com.engboost.encryptedca.core.certificates

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Main-safe access to certificate profiles; one instance per app. Calls run one at a time, and
 * changes can't be cancelled halfway, so a Keystore write always completes or rolls back.
 */
class CertificateProfileRepository internal constructor(
    private val store: CertificateProfileStore,
    private val ioDispatcher: CoroutineDispatcher,
) {
    constructor(context: Context) : this(CertificateProfileStore(context.applicationContext), Dispatchers.IO)

    private val mutex = Mutex()
    private val _index = MutableStateFlow<ProfileIndex?>(null)

    /** `null` until the first [refresh] or change. */
    val index: StateFlow<ProfileIndex?> = _index.asStateFlow()

    suspend fun refresh() = change {}

    /** Takes ownership of [p12] and [password] and wipes them. */
    suspend fun importProfile(displayName: String?, p12: ByteArray, password: CharArray, caPem: ByteArray): String =
        change { store -> store.importProfile(displayName, p12, password, caPem) }

    suspend fun selectProfile(profileId: String) = change { store -> store.setActiveProfile(profileId) }

    suspend fun deleteProfile(profileId: String) = change { store -> store.deleteProfile(profileId) }

    /** Credentials of the selected profile, or `null` when none is selected. */
    suspend fun activeCredentials(): ClientCredentials? =
        withContext(ioDispatcher) {
            mutex.withLock { store.activeCredentials() }
        }

    private suspend fun <T> change(block: (CertificateProfileStore) -> T): T =
        withContext(ioDispatcher + NonCancellable) {
            mutex.withLock {
                val result = runCatching { block(store) }
                val index = runCatching { store.readIndex() }
                index.onSuccess { _index.value = it }
                result.onFailure { failure ->
                    index.exceptionOrNull()?.let(failure::addSuppressed)
                    throw failure
                }
                index.getOrThrow()
                result.getOrThrow()
            }
        }
}
