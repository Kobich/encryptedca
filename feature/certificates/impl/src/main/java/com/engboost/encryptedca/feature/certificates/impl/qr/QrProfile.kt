package com.engboost.encryptedca.feature.certificates.impl.qr

internal class QrProfile(val p12: ByteArray, val caCertificate: ByteArray) {
    fun wipe() {
        p12.fill(0)
        caCertificate.fill(0)
    }
}
