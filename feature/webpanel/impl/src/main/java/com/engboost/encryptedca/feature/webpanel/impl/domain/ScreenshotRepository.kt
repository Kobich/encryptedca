package com.engboost.encryptedca.feature.webpanel.impl.domain

import android.net.Uri

internal interface ScreenshotRepository {
    suspend fun saveDataUrl(dataUrl: String): Uri?
}
