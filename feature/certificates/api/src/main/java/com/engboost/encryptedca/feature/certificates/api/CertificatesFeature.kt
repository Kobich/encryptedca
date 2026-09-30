// Certificate profiles for the UI: the list, selecting and deleting a profile, importing a new one.
// Errors come back as a CertificateProfileError or an ImportResult instead of an exception.
// Imports take ownership of the password and wipe it whatever the outcome.
package com.engboost.encryptedca.feature.certificates.api

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import kotlinx.coroutines.flow.Flow

interface CertificatesFeature {
    val profiles: Flow<ProfileIndex>

    suspend fun refreshProfiles(): CertificateProfileError?

    suspend fun selectProfile(profileId: String): CertificateProfileError?

    suspend fun deleteProfile(profileId: String): CertificateProfileError?

    suspend fun documentName(uri: Uri): String?

    suspend fun importFromFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportResult

    suspend fun readQrCodes(uris: List<Uri>): List<String>

    fun newQrCollection(): QrCollection
}
