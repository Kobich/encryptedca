package com.engboost.encryptedca.certificates;

import static org.junit.Assert.*;

import android.content.Context;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Exercises AndroidKeyStore and therefore runs only as an instrumentation test. */
@RunWith(AndroidJUnit4.class)
public final class CertificateProfileStoreInstrumentedTest {
    private static final String FIXTURE_PASSWORD = "fixture-password";
    // A test-only self-signed CA/private-key PKCS#12. It is not part of the production APK.
    private static final String P12_BASE64 = "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";
    private static final String CA_DER_BASE64 = "MIIByDCCATGgAwIBAgIITFauUCNNGrIwDQYJKoZIhvcNAQELBQAwHDEaMBgGA1UEAxMRRW5jcnlwdGVkIENBIFRlc3QwHhcNMjYwOTIxMTEwODU5WhcNMjcwOTIyMTEwODU5WjAcMRowGAYDVQQDExFFbmNyeXB0ZWQgQ0EgVGVzdDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA03a8DXfGgAbBQ6PVktv4MDDUYr/T3E91/+4hRAd/+0u8W4GGI/2nrxWwr99W2ke4Suyo1VEdhlqD0MU2VvLcn7Q5Wuhy0vfvlnlmvJavJd+9S0AQ7JCY7fboA8XCfkFXRVNB3w48x9bER5VRAfDKEr9U0I4oQIOfZsli79GeTcUCAwEAAaMTMBEwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOBgQC6dLX2hgs1tvuwz4DftspuSDZZMkck7xaUrrXd7lwWrz8FGiO/rc8K/YCQf7UtPbS86ZB7d8MKtNZv84nb3n3d/VelGI9tSj/wNImAaB2D45u4RWVf4jLgI+LRGDY9uN/iMZ1KChaMcQRAU2ynIgD8e658Xtr8GFRBYff9WXg6ew==";

    private Context context;
    private CertificateProfileStore store;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        store = new CertificateProfileStore(context);
        clearProfiles();
    }

    @After public void tearDown() {
        clearProfiles();
    }

    @Test public void importsValidProfile() {
        String id = importFixture("First");
        assertTrue(store.containsProfile(id));
        assertEquals(1, store.getProfileIds().size());
        assertTrue(store.getProfileIds().contains(id));
        assertEquals("First", store.getProfile(id).getDisplayName());
        assertEquals("mtls_client_" + id, store.getProfile(id).getClientKeyAlias());
    }

    @Test public void rejectsWrongP12PasswordAndClearsIt() {
        char[] wrongPassword = "wrong-password".toCharArray();
        try {
            store.importProfile("Bad", p12(), wrongPassword, caPem());
            fail("Expected an invalid PKCS#12 password");
        } catch (CertificateProfileException expected) {
            assertTrue(expected.getMessage().contains("password"));
        }
        for (char value : wrongPassword) assertEquals('\0', value);
    }

    @Test public void keepsSeparateAliasesAndSelectedProfileAfterRecreation() {
        String first = importFixture("First");
        String second = importFixture("Second");
        assertNotEquals(store.getProfile(first).getClientKeyAlias(), store.getProfile(second).getClientKeyAlias());

        store.setActiveProfile(second);
        CertificateProfileStore recreated = new CertificateProfileStore(context);
        assertEquals(2, recreated.getProfileIds().size());
        assertTrue(recreated.getProfileIds().contains(first));
        assertTrue(recreated.getProfileIds().contains(second));
        assertEquals(second, recreated.getActiveProfileId());
        assertEquals(second, recreated.getActiveProfile().getProfileId());
    }

    @Test public void createsSslContextForSelectedProfile() {
        String first = importFixture("First");
        String second = importFixture("Second");
        assertNotEquals(first, second);
        assertNotNull(new ProfileSslContextFactory().create(store, second));
    }

    @Test public void deletesProfileAndClearsActiveProfile() {
        String id = importFixture("To delete");
        store.setActiveProfile(id);
        store.deleteProfile(id);
        assertFalse(store.containsProfile(id));
        assertNull(store.getActiveProfileId());
    }

    private String importFixture(String name) {
        return store.importProfile(name, p12(), FIXTURE_PASSWORD.toCharArray(), caPem());
    }

    private static ByteArrayInputStream p12() {
        return new ByteArrayInputStream(Base64.decode(P12_BASE64, Base64.DEFAULT));
    }

    private static ByteArrayInputStream caPem() {
        String pem = "-----BEGIN CERTIFICATE-----\n" + CA_DER_BASE64
                + "\n-----END CERTIFICATE-----\n";
        return new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII));
    }

    private void clearProfiles() {
        for (String id : store.getProfileIds()) store.deleteProfile(id);
    }
}
