package com.engboost.encryptedca.feature.certificates.add.qr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ML Kit's QR detector; the model is bundled in the APK, so Google Play services aren't needed. */
internal fun createQrScanner(): BarcodeScanner =
    BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())

/** Reads every QR code on a picture, e.g. a screenshot or a photo of the printed sheet. */
internal class QrImageReader(private val context: Context) {

    /** The texts of the codes found; empty when there are none or the image can't be opened. */
    suspend fun read(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val scanner = createQrScanner()
        try {
            val image = InputImage.fromFilePath(context, uri)
            Tasks.await(scanner.process(image)).mapNotNull { it.rawValue }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not read QR codes from $uri", e)
            emptyList()
        } finally {
            scanner.close()
        }
    }

    private companion object {
        const val TAG = "QrImageReader"
    }
}
