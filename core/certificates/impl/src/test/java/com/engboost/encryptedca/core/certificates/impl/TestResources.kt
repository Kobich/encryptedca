// Test certificates come from tools/test-certs/make_test_certs.sh; the .p12 password is "test".
package com.engboost.encryptedca.core.certificates.impl

internal fun resource(name: String): ByteArray =
    checkNotNull(object {}.javaClass.classLoader?.getResourceAsStream(name)) { "Missing test resource $name" }
        .use { it.readBytes() }

internal fun testPassword() = "test".toCharArray()
