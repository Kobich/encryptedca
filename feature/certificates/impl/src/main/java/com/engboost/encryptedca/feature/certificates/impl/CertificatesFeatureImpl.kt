package com.engboost.encryptedca.feature.certificates.impl

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.feature.certificates.api.CertificatesFeature
import com.engboost.encryptedca.feature.certificates.api.QrCollection
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import com.engboost.encryptedca.feature.certificates.impl.domain.CertificatesInteractor
import kotlinx.coroutines.flow.Flow

internal class CertificatesFeatureImpl(
    private val interactor: CertificatesInteractor,
) : CertificatesFeature {
    override val profiles: Flow<ProfileIndex> = interactor.profiles

    override suspend fun refreshProfiles(): CertificateProfileError? = interactor.refresh()

    override suspend fun selectProfile(profileId: String): CertificateProfileError? = interactor.select(profileId)

    override suspend fun deleteProfile(profileId: String): CertificateProfileError? = interactor.delete(profileId)

    override suspend fun documentName(uri: Uri): String? = interactor.documentName(uri)

    override suspend fun importFromFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportResult =
        interactor.importFromFiles(displayName, p12Uri, caUri, password)

    override suspend fun readQrCodes(uris: List<Uri>): List<String> = interactor.readQrCodes(uris)

    override fun newQrCollection(): QrCollection = QrCollectionImpl(interactor)
}
