package com.engboost.encryptedca.feature.certificates.impl.interactor

import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.model.ProfileIndex
import com.engboost.encryptedca.feature.certificates.api.ProfileListInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

internal class DefaultProfileListInteractor(
    private val repository: CertificateProfileRepository,
) : ProfileListInteractor {

    override val profiles: Flow<ProfileIndex> = repository.index.filterNotNull()

    override suspend fun refresh(): CertificateProfileError? = errorOf { repository.refresh() }

    override suspend fun select(profileId: String): CertificateProfileError? = errorOf { repository.selectProfile(profileId) }

    override suspend fun delete(profileId: String): CertificateProfileError? = errorOf { repository.deleteProfile(profileId) }

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
