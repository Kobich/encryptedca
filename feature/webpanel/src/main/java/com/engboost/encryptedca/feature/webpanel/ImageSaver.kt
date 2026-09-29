// Сохраняет в DCIM скриншоты, которые веб-интерфейс устройства отдаёт загрузкой data:image/<тип>;base64,<данные>.
// Пока файл пишется, он скрыт от других приложений (IS_PENDING). Недописанный файл удаляется из галереи.
// Возвращает null, если это не картинка в base64 или запись не удалась.
package com.engboost.encryptedca.feature.webpanel

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal class ImageSaver(private val resolver: ContentResolver) {

    suspend fun saveDataUrl(dataUrl: String): Uri? = withContext(Dispatchers.IO) {
        val image = decode(dataUrl) ?: return@withContext null
        val entry = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.${image.extension}")
            put(MediaStore.Images.Media.MIME_TYPE, image.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, entry)
        } catch (e: Exception) {
            Log.w(TAG, "Could not create a gallery entry", e)
            null
        } ?: return@withContext null

        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("No output stream for $uri")
            output.use { it.write(image.bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            Log.w(TAG, "Could not write the image", e)
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    private class DecodedImage(val mimeType: String, val extension: String, val bytes: ByteArray)

    private fun decode(dataUrl: String): DecodedImage? {
        val header = dataUrl.substringBefore(',', missingDelimiterValue = "")
        if (!header.startsWith("data:image/") || !header.endsWith(";base64")) return null
        val mimeType = header.removePrefix("data:").substringBefore(';')
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: return null
        val bytes = try {
            Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return DecodedImage(mimeType, extension, bytes)
    }

    private companion object {
        const val TAG = "ImageSaver"
    }
}
