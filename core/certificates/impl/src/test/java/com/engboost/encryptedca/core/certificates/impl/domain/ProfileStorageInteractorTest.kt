package com.engboost.encryptedca.core.certificates.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.PROFILE_INCOMPLETE
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError.STORAGE_FAILED
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.core.certificates.api.entity.ProfileSummary
import com.engboost.encryptedca.core.certificates.impl.domain.parsing.PrivateKeyWithChain
import com.engboost.encryptedca.core.certificates.impl.resource
import com.engboost.encryptedca.core.certificates.impl.testPassword
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate

class ProfileStorageInteractorTest {
    private val keys = FakeKeys()
    private val caCertificates = FakeCaCertificates()
    private val profiles = FakeProfileIndex()
    private val interactor = ProfileStorageInteractor(keys, caCertificates, profiles)

    @Test
    fun importStoresAllThreePartsAndWipesThePassword() = runTest {
        val password = testPassword()

        val id = import(password)

        assertTrue(keys.exists(id) && caCertificates.exists(id) && profiles.contains(id))
        assertEquals(listOf("Lab"), interactor.index.value!!.profiles.map { it.displayName })
        assertTrue(password.all { it == '\u0000' })
    }

    @Test
    fun failedCaWriteRemovesTheSavedKey() = runTest {
        caCertificates.failWrite = true

        assertError(STORAGE_FAILED) { import() }
        assertTrue(keys.stored.isEmpty())
    }

    @Test
    fun failedRegistrationRemovesKeyAndCa() = runTest {
        profiles.failRegister = true

        assertError(STORAGE_FAILED) { import() }
        assertTrue(keys.stored.isEmpty())
        assertTrue(caCertificates.stored.isEmpty())
    }

    @Test
    fun wrongPasswordStoresNothingAndWipesThePassword() = runTest {
        val password = "wrong".toCharArray()

        assertError(PKCS12_PASSWORD_OR_CORRUPT) { import(password) }
        assertTrue(keys.stored.isEmpty())
        assertTrue(password.all { it == '\u0000' })
    }

    @Test
    fun selectingUnknownProfileKeepsTheActiveOne() = runTest {
        val id = import()
        interactor.selectProfile(id)

        assertError(PROFILE_INCOMPLETE) { interactor.selectProfile("unknown") }
        assertEquals(id, interactor.index.value!!.activeProfileId)
    }

    @Test
    fun deletingTheActiveProfileLeavesNoCredentials() = runTest {
        val id = import()
        interactor.selectProfile(id)
        assertEquals(id, interactor.loadActiveCredentials()?.profileId)

        interactor.deleteProfile(id)

        assertNull(interactor.loadActiveCredentials())
        assertTrue(keys.stored.isEmpty() && caCertificates.stored.isEmpty())
    }

    private suspend fun import(password: CharArray = testPassword()) =
        interactor.importProfile("Lab", resource("client.p12"), password, resource("ca.pem"))

    private suspend fun assertError(expected: CertificateProfileError, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CertificateProfileException) {
            assertEquals(expected, e.error)
            return
        }
        fail("Expected $expected")
    }
}

private class FakeKeys : ClientKeyRepository {
    val stored = HashMap<String, PrivateKeyWithChain>()

    override fun save(profileId: String, key: PrivateKeyWithChain) {
        stored[profileId] = key
    }

    override fun read(profileId: String): KeyStore.PrivateKeyEntry {
        val key = stored.getValue(profileId)
        return KeyStore.PrivateKeyEntry(key.privateKey, arrayOf<Certificate>(*key.certificateChain))
    }

    override fun exists(profileId: String) = profileId in stored

    override fun delete(profileId: String) {
        stored.remove(profileId)
    }
}

private class FakeCaCertificates : CaCertificateRepository {
    val stored = HashMap<String, X509Certificate>()
    var failWrite = false

    override fun write(profileId: String, caCertificate: X509Certificate) {
        if (failWrite) throw IOException("Disk is full")
        stored[profileId] = caCertificate
    }

    override fun read(profileId: String) = stored.getValue(profileId)

    override fun exists(profileId: String) = profileId in stored

    override fun delete(profileId: String) {
        stored.remove(profileId)
    }
}

private class FakeProfileIndex : ProfileIndexRepository {
    private val names = LinkedHashMap<String, String?>()
    var failRegister = false

    override var activeProfileId: String? = null
        private set

    override fun read() = ProfileIndex(names.map { (id, name) -> ProfileSummary(id, name, createdAt = 0) }, activeProfileId)

    override fun contains(profileId: String) = profileId in names

    override fun setActive(profileId: String) {
        activeProfileId = profileId
    }

    override fun register(profileId: String, displayName: String?) {
        if (failRegister) throw IOException("Could not commit")
        names[profileId] = displayName
    }

    override fun unregister(profileId: String) {
        names.remove(profileId)
        if (activeProfileId == profileId) activeProfileId = null
    }
}
