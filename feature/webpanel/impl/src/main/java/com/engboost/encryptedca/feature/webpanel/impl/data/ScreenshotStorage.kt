package com.engboost.encryptedca.feature.webpanel.impl.data

import android.net.Uri

internal interface ScreenshotStorage {
    suspend fun saveDataUrl(dataUrl: String): Uri?
}
