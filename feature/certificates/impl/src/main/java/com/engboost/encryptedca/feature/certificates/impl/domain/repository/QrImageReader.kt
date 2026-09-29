package com.engboost.encryptedca.feature.certificates.impl.domain.repository

import android.net.Uri

internal interface QrImageReader {
    suspend fun read(uri: Uri): List<String>
}
