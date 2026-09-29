package com.engboost.encryptedca.feature.certificates.impl.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal class ContentResolverDocumentRepository(private val resolver: ContentResolver) : DocumentRepository {

    override suspend fun displayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }

    override suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("Provider returned no stream")
        } catch (e: IOException) {
            throw unavailable(e)
        } catch (e: SecurityException) {
            throw unavailable(e)
        }
    }

    private fun unavailable(cause: Exception) =
        CertificateProfileException(CertificateProfileError.FILE_UNAVAILABLE, "Selected document could not be read", cause)
}
