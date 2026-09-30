package com.engboost.encryptedca.core.certificates.impl.domain.parsing

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.CERTIFICATE_INVALID
import com.engboost.encryptedca.core.certificates.impl.domain.rethrowAs
import java.security.cert.X509Certificate

internal fun X509Certificate.requireCurrentlyValid() =
    rethrowAs(CERTIFICATE_INVALID, "Certificate is expired or not yet valid") { checkValidity() }
