// Собирает профиль из QR-кодов, которые приходят с камеры или из фото.
// Собранный профиль хранится только в памяти и затирается при сбросе.
package com.engboost.encryptedca.feature.certificates.add.qr

import android.util.Log
import com.engboost.encryptedca.feature.certificates.add.QrStatus

internal class QrProfileCollector {
    private val assembler = QrProfileAssembler()

    var profile: QrProfile? = null
        private set

    fun progress(photosWithoutNewCodes: Boolean = false) =
        QrStatus.Collecting(assembler.received, assembler.total, photosWithoutNewCodes)

    fun add(texts: List<String>): QrStatus? {
        val anyNew = texts.map(assembler::add).any { it }
        if (!anyNew) return null
        if (!assembler.complete) return progress()
        val status = try {
            profile = assembler.assemble()
            QrStatus.Ready
        } catch (e: QrFormatException) {
            Log.w(TAG, "QR codes don't form a profile", e)
            QrStatus.Invalid
        }
        assembler.reset()
        return status
    }

    fun clear() {
        assembler.reset()
        profile?.wipe()
        profile = null
    }

    private companion object {
        const val TAG = "QrProfileCollector"
    }
}
