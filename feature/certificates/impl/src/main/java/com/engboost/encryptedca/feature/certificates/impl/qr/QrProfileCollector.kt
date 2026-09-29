// Collects a profile from QR codes coming from the camera or from photos.
// The collected profile stays in memory only and is wiped on clear().
package com.engboost.encryptedca.feature.certificates.impl.qr

import com.engboost.encryptedca.feature.certificates.api.model.QrCollectResult

internal class QrProfileCollector {
    private val assembler = QrProfileAssembler()

    val received: Int get() = assembler.received
    val total: Int get() = assembler.total

    var profile: QrProfile? = null
        private set

    fun add(texts: List<String>): QrCollectResult {
        val anyNew = texts.map(assembler::add).any { it }
        if (!anyNew) return QrCollectResult.NothingNew
        if (!assembler.complete) return QrCollectResult.Collecting
        val result = try {
            profile = assembler.assemble()
            QrCollectResult.Complete
        } catch (e: QrFormatException) {
            QrCollectResult.Invalid(e)
        }
        assembler.reset()
        return result
    }

    fun clear() {
        assembler.reset()
        profile?.wipe()
        profile = null
    }
}
