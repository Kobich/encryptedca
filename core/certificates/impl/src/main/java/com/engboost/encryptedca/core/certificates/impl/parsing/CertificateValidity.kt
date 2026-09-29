package com.engboost.encryptedca.core.certificates.impl.parsing

import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError.CERTIFICATE_INVALID
import com.engboost.encryptedca.core.certificates.impl.rethrowAs
import java.security.cert.X509Certificate

internal fun X509Certificate.requireCurrentlyValid() =
    rethrowAs(CERTIFICATE_INVALID, "Certificate is expired or not yet valid") { checkValidity() }
