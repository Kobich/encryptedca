// One profile being read from QR codes. The collected profile stays in memory only and is wiped by clear().
package com.engboost.encryptedca.feature.certificates.api

import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import com.engboost.encryptedca.feature.certificates.api.entity.QrCollectResult

interface QrCollection {
    val received: Int
    val total: Int

    fun add(texts: List<String>): QrCollectResult

    suspend fun importProfile(displayName: String?, password: CharArray): ImportResult

    fun clear()
}
