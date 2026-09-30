// Certificate profiles for the UI: the list, selecting and deleting a profile, importing a new one.
// Errors come back as a CertificateProfileError or an ImportResult instead of an exception.
// Imports take ownership of the password and wipe it whatever the outcome.
// Documents and photos are content URIs as strings.
package com.engboost.encryptedca.feature.certificates.api

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import kotlinx.coroutines.flow.Flow

interface CertificatesFeature {
    val profiles: Flow<ProfileIndex>

    suspend fun refreshProfiles(): CertificateProfileError?

    suspend fun selectProfile(profileId: String): CertificateProfileError?

    suspend fun deleteProfile(profileId: String): CertificateProfileError?

    suspend fun documentName(uri: String): String?

    suspend fun importFromFiles(displayName: String?, p12Uri: String, caUri: String, password: CharArray): ImportResult

    suspend fun readQrCodes(uris: List<String>): List<String>

    fun newQrCollection(): QrCollection
}
