// Operations of the profile list. Errors come back as a CertificateProfileError instead of an exception.
package com.engboost.encryptedca.feature.certificates.impl.domain.interactor

import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.model.ProfileIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

internal class ProfileListInteractor(private val repository: CertificateProfileRepository) {

    val profiles: Flow<ProfileIndex> = repository.index.filterNotNull()

    suspend fun refresh(): CertificateProfileError? = errorOf { repository.refresh() }

    suspend fun select(profileId: String): CertificateProfileError? = errorOf { repository.selectProfile(profileId) }

    suspend fun delete(profileId: String): CertificateProfileError? = errorOf { repository.deleteProfile(profileId) }

    private suspend fun errorOf(block: suspend () -> Unit): CertificateProfileError? =
        try {
            block()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: CertificateProfileException) {
            e.error
        } catch (e: Exception) {
            CertificateProfileError.STORAGE_FAILED
        }
}
