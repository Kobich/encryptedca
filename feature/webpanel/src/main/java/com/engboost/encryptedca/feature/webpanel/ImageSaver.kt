package com.engboost.encryptedca.feature.webpanel

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** The device's web UI hands out screenshots as `data:image/...;base64,...` downloads. */
internal class ImageSaver(private val resolver: ContentResolver) {

    /** Saves the image to DCIM; returns `null` for anything that isn't a base64 image or can't be written. */
    suspend fun saveDataUrl(dataUrl: String): Uri? = withContext(Dispatchers.IO) {
        if (!dataUrl.startsWith(DATA_IMAGE_PREFIX)) return@withContext null
        val mimeType = dataUrl.removePrefix("data:").substringBefore(';')
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.$extension")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
        }
        try {
            val bytes = Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext null
            uri
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    private companion object {
        const val DATA_IMAGE_PREFIX = "data:image/"
    }
}
