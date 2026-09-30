package com.engboost.encryptedca.core.network.api

import java.security.MessageDigest
import java.security.cert.X509Certificate

fun X509Certificate.sha256Fingerprint(): String =
    MessageDigest.getInstance("SHA-256").digest(encoded).joinToString("") { "%02x".format(it) }
