// Certificate profile operations. Errors are turned into CertificateProfileError / ImportResult here.
// The password is always wiped, whatever the outcome of an import.
// The repository wipes the bytes it gets, so a QR profile is passed as a copy:
// after a wrong password the codes needn't be scanned again.
package com.engboost.encryptedca.feature.certificates.impl.domain

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.ProfileStorageFeature
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import com.engboost.encryptedca.feature.certificates.impl.domain.qr.QrProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

internal class CertificatesInteractor(
    private val profileStorage: ProfileStorageFeature,
    private val documentRepository: DocumentRepository,
    private val qrImageRepository: QrImageRepository,
) {
    val profiles: Flow<ProfileIndex> = profileStorage.index.filterNotNull()

    suspend fun refresh(): CertificateProfileError? = errorOf { profileStorage.refresh() }

    suspend fun select(profileId: String): CertificateProfileError? = errorOf { profileStorage.selectProfile(profileId) }

    suspend fun delete(profileId: String): CertificateProfileError? = errorOf { profileStorage.deleteProfile(profileId) }

    suspend fun documentName(uri: Uri): String? = documentRepository.displayName(uri)

    suspend fun readQrCodes(uris: List<Uri>): List<String> = uris.flatMap { qrImageRepository.read(it) }

    suspend fun importFromFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportResult =
        runImport(password) {
            val p12 = documentRepository.read(p12Uri)
            try {
                profileStorage.importProfile(displayName, p12, password, documentRepository.read(caUri))
            } finally {
                p12.fill(0)
            }
        }

    suspend fun importFromQr(displayName: String?, qr: QrProfile, password: CharArray): ImportResult =
        runImport(password) {
            val p12 = qr.p12.copyOf()
            try {
                profileStorage.importProfile(displayName, p12, password, qr.caCertificate.copyOf())
            } finally {
                p12.fill(0)
            }
        }

    private suspend fun runImport(password: CharArray, import: suspend () -> String): ImportResult =
        try {
            ImportResult.Imported(import())
        } catch (e: CancellationException) {
            throw e
        } catch (e: CertificateProfileException) {
            ImportResult.Failed(e.error, e)
        } catch (e: Exception) {
            ImportResult.Failed(CertificateProfileError.STORAGE_FAILED, e)
        } finally {
            password.wipe()
        }

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
