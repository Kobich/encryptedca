// Stored certificate profiles: the list, the active profile, import and delete.
// importProfile takes ownership of the .p12 bytes and the password and wipes them whatever the outcome.
package com.engboost.encryptedca.core.certificates.api

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import kotlinx.coroutines.flow.StateFlow

interface ProfileStorageFeature {
    val index: StateFlow<ProfileIndex?>

    suspend fun refresh()

    suspend fun importProfile(displayName: String?, p12: ByteArray, password: CharArray, caPem: ByteArray): String

    suspend fun selectProfile(profileId: String)

    suspend fun deleteProfile(profileId: String)

    suspend fun loadActiveCredentials(): ClientCredentials?
}
