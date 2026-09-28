package com.engboost.encryptedca.core.network.tls

import java.security.MessageDigest
import java.security.cert.X509Certificate

/** Hex SHA-256 of the DER encoding: identifies the exact certificate a device presented. */
fun X509Certificate.sha256Fingerprint(): String =
    MessageDigest.getInstance("SHA-256").digest(encoded).joinToString("") { "%02x".format(it) }
