// Operations of the profile list. Errors come back as a CertificateProfileError instead of an exception.
package com.engboost.encryptedca.feature.certificates.api

import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.ProfileIndex
import kotlinx.coroutines.flow.Flow

interface ProfileListInteractor {
    val profiles: Flow<ProfileIndex>

    suspend fun refresh(): CertificateProfileError?

    suspend fun select(profileId: String): CertificateProfileError?

    suspend fun delete(profileId: String): CertificateProfileError?
}
