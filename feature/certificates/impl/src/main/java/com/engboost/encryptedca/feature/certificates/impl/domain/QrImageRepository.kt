package com.engboost.encryptedca.feature.certificates.impl.domain

import android.net.Uri

internal interface QrImageRepository {
    suspend fun read(uri: Uri): List<String>
}
