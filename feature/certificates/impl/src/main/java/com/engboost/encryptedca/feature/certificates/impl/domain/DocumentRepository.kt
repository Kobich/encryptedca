package com.engboost.encryptedca.feature.certificates.impl.domain

import android.net.Uri

internal interface DocumentRepository {
    suspend fun displayName(uri: Uri): String?

    suspend fun read(uri: Uri): ByteArray
}
