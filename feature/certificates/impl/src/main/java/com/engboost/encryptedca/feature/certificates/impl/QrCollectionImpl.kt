package com.engboost.encryptedca.feature.certificates.impl

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.feature.certificates.api.QrCollection
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import com.engboost.encryptedca.feature.certificates.api.entity.QrCollectResult
import com.engboost.encryptedca.feature.certificates.impl.domain.CertificatesInteractor
import com.engboost.encryptedca.feature.certificates.impl.domain.qr.QrProfileCollector

internal class QrCollectionImpl(
    private val interactor: CertificatesInteractor,
) : QrCollection {
    private val collector = QrProfileCollector()

    override val received: Int get() = collector.received
    override val total: Int get() = collector.total

    override fun add(texts: List<String>): QrCollectResult = collector.add(texts)

    override suspend fun importProfile(displayName: String?, password: CharArray): ImportResult {
        val profile = collector.profile
        if (profile == null) {
            password.wipe()
            return ImportResult.Failed(CertificateProfileError.STORAGE_FAILED, IllegalStateException("No QR profile collected"))
        }
        return interactor.importFromQr(displayName, profile, password)
    }

    override fun clear() = collector.clear()
}
