package com.engboost.encryptedca.core.certificates.parsing

import com.engboost.encryptedca.core.certificates.model.CertificateProfileError.CERTIFICATE_INVALID
import com.engboost.encryptedca.core.certificates.model.rethrowAs
import java.security.cert.X509Certificate

/** Checked on import and again whenever stored credentials are loaded. */
internal fun X509Certificate.requireCurrentlyValid() =
    rethrowAs(CERTIFICATE_INVALID, "Certificate is expired or not yet valid") { checkValidity() }
