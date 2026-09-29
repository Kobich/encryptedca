// Everything the add-profile screen needs: names of picked files, collecting QR codes and the import itself.
// The collected QR profile stays in memory only and is wiped by clearQr().
// The password is always wiped, whatever the outcome.
// The repository wipes the bytes it gets, so the QR profile is passed as a copy:
// after a wrong password the codes needn't be scanned again.
package com.engboost.encryptedca.feature.certificates.impl.domain.interactor

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.feature.certificates.impl.domain.model.ImportResult
import com.engboost.encryptedca.feature.certificates.impl.domain.qr.QrCollectResult
import com.engboost.encryptedca.feature.certificates.impl.domain.qr.QrProfileCollector
import com.engboost.encryptedca.feature.certificates.impl.domain.repository.DocumentRepository
import com.engboost.encryptedca.feature.certificates.impl.domain.repository.QrImageReader
import kotlinx.coroutines.CancellationException

internal class AddProfileInteractor(
    private val repository: CertificateProfileRepository,
    private val documents: DocumentRepository,
    private val qrImages: QrImageReader,
) {
    private val qrCollector = QrProfileCollector()

    val qrReceived: Int get() = qrCollector.received
    val qrTotal: Int get() = qrCollector.total

    suspend fun documentName(uri: Uri): String? = documents.displayName(uri)

    suspend fun readQrCodes(uris: List<Uri>): List<String> = uris.flatMap { qrImages.read(it) }

    fun addQrCodes(texts: List<String>): QrCollectResult = qrCollector.add(texts)

    fun clearQr() = qrCollector.clear()

    suspend fun importFromFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportResult =
        runImport(password) {
            val p12 = documents.read(p12Uri)
            try {
                repository.importProfile(displayName, p12, password, documents.read(caUri))
            } finally {
                p12.fill(0)
            }
        }

    suspend fun importFromQr(displayName: String?, password: CharArray): ImportResult =
        runImport(password) {
            val qr = checkNotNull(qrCollector.profile) { "No QR profile collected" }
            val p12 = qr.p12.copyOf()
            try {
                repository.importProfile(displayName, p12, password, qr.caCertificate.copyOf())
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
}
