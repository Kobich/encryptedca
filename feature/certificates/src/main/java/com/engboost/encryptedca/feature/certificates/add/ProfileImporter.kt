// Импорт профиля в репозиторий: из двух выбранных файлов или из профиля, собранного по QR-кодам.
// Пароль затирается всегда, чем бы импорт ни закончился.
// Репозиторий затирает полученные байты, поэтому профиль из QR отдаётся копией:
// после неверного пароля коды не нужно сканировать заново.
package com.engboost.encryptedca.feature.certificates.add

import android.net.Uri
import android.util.Log
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.wipe
import com.engboost.encryptedca.feature.certificates.add.qr.QrProfile
import kotlinx.coroutines.CancellationException

internal class ProfileImporter(
    private val repository: CertificateProfileRepository,
    private val documents: DocumentReader,
) {

    suspend fun importFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportStatus =
        runImport(password) {
            val p12 = documents.read(p12Uri)
            try {
                repository.importProfile(displayName, p12, password, documents.read(caUri))
            } finally {
                p12.fill(0)
            }
        }

    suspend fun importQr(displayName: String?, qr: QrProfile, password: CharArray): ImportStatus =
        runImport(password) {
            val p12 = qr.p12.copyOf()
            try {
                repository.importProfile(displayName, p12, password, qr.caCertificate.copyOf())
            } finally {
                p12.fill(0)
            }
        }

    private suspend fun runImport(password: CharArray, import: suspend () -> String): ImportStatus =
        try {
            ImportStatus.Imported(import())
        } catch (e: CancellationException) {
            throw e
        } catch (e: CertificateProfileException) {
            Log.w(TAG, "Import failed: ${e.error}", e)
            ImportStatus.Failed(e.error)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected import failure", e)
            ImportStatus.Failed(CertificateProfileError.STORAGE_FAILED)
        } finally {
            password.wipe()
        }

    private companion object {
        const val TAG = "ProfileImporter"
    }
}
