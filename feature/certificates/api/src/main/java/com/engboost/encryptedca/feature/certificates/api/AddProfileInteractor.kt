// Everything the add-profile screen needs: names of picked files, collecting QR codes and the import itself.
// The collected QR profile stays in memory only and is wiped by clearQr().
// Both imports take ownership of the password and wipe it whatever the outcome.
package com.engboost.encryptedca.feature.certificates.api

import android.net.Uri
import com.engboost.encryptedca.feature.certificates.api.model.ImportResult
import com.engboost.encryptedca.feature.certificates.api.model.QrCollectResult

interface AddProfileInteractor {
    val qrReceived: Int
    val qrTotal: Int

    suspend fun documentName(uri: Uri): String?

    suspend fun readQrCodes(uris: List<Uri>): List<String>

    fun addQrCodes(texts: List<String>): QrCollectResult

    fun clearQr()

    suspend fun importFromFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportResult

    suspend fun importFromQr(displayName: String?, password: CharArray): ImportResult
}
