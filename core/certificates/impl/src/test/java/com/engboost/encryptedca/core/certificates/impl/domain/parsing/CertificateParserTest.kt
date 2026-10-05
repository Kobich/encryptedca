package com.engboost.encryptedca.core.certificates.impl.domain.parsing

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.CERTIFICATE_INVALID
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.certificates.impl.resource
import com.engboost.encryptedca.core.certificates.impl.testPassword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CertificateParserTest {

    @Test
    fun readsKeyAndChainFromP12() {
        val key = CertificateParser.readPkcs12(resource("client.p12"), testPassword())

        assertEquals("RSA", key.privateKey.algorithm)
        assertEquals("CN=Test client", key.certificateChain.first().subjectX500Principal.name)
    }

    @Test
    fun wrongPasswordIsReported() {
        assertError(PKCS12_PASSWORD_OR_CORRUPT) { CertificateParser.readPkcs12(resource("client.p12"), "wrong".toCharArray()) }
    }

    @Test
    fun corruptP12IsReported() {
        assertError(PKCS12_PASSWORD_OR_CORRUPT) { CertificateParser.readPkcs12(ByteArray(64) { it.toByte() }, testPassword()) }
    }

    @Test
    fun readsCaPem() {
        assertEquals("CN=Test ca", CertificateParser.readCaPem(resource("ca.pem")).subjectX500Principal.name)
    }

    @Test
    fun expiredCaIsInvalid() {
        assertError(CERTIFICATE_INVALID) { CertificateParser.readCaPem(resource("expired.pem")) }
    }

    @Test
    fun garbageInsteadOfCaIsInvalid() {
        assertError(CERTIFICATE_INVALID) { CertificateParser.readCaPem("not a certificate".toByteArray()) }
    }

    private fun assertError(expected: CertificateProfileError, block: () -> Unit) {
        assertEquals(expected, assertThrows(CertificateProfileException::class.java, block).error)
    }
}
