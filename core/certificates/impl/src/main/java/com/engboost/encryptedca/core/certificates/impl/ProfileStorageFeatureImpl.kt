package com.engboost.encryptedca.core.certificates.impl

import com.engboost.encryptedca.core.certificates.api.ProfileStorageFeature
import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.core.certificates.impl.domain.ProfileStorageInteractor
import kotlinx.coroutines.flow.StateFlow

internal class ProfileStorageFeatureImpl(
    private val interactor: ProfileStorageInteractor,
) : ProfileStorageFeature {
    override val index: StateFlow<ProfileIndex?> = interactor.index

    override suspend fun refresh() = interactor.refresh()

    override suspend fun importProfile(displayName: String?, p12: ByteArray, password: CharArray, caPem: ByteArray): String =
        interactor.importProfile(displayName, p12, password, caPem)

    override suspend fun selectProfile(profileId: String) = interactor.selectProfile(profileId)

    override suspend fun deleteProfile(profileId: String) = interactor.deleteProfile(profileId)

    override suspend fun loadActiveCredentials(): ClientCredentials? = interactor.loadActiveCredentials()
}
