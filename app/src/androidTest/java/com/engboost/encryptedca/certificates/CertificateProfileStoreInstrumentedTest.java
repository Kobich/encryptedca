package com.engboost.encryptedca.certificates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.app.Application;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Observer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public final class CertificateProfileStoreInstrumentedTest {
    private static final String PASSWORD = "changeit";
    private Context targetContext;
    private CertificateProfileStore store;

    @Before public void setUp() {
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        store = new CertificateProfileStore(targetContext);
        clearProfiles(store);
    }

    @After public void tearDown() {
        clearProfiles(store);
    }

    @Test public void importsAndReadsProfileAfterStoreRecreation() throws Exception {
        String profileId = importFixture(store, "First");
        CertificateProfile profile = store.getProfile(profileId);
        assertEquals("First", profile.getDisplayName());
        assertEquals("mtls_client_" + profileId, profile.getClientKeyAlias());
        assertNotNull(profile.getCaCertificate());

        CertificateProfileStore recreated = new CertificateProfileStore(targetContext);
        assertTrue(recreated.containsProfile(profileId));
        assertNotNull(recreated.getProfile(profileId).getCaCertificate());
    }

    @Test public void clearsPasswordAfterInvalidPasswordAndDoesNotRegisterProfile() throws Exception {
        char[] password = "wrong-password".toCharArray();
        try {
            store.importProfile("Bad", asset("client.p12"), password, asset("ca.pem"));
            fail("Expected PKCS#12 failure");
        } catch (CertificateProfileException expected) {
            assertEquals(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT, expected.getError());
        }
        assertCleared(password);
        assertTrue(store.getProfileIds().isEmpty());
    }

    @Test public void rejectsCorruptedInputsWithoutRegisteringProfile() throws Exception {
        char[] password = PASSWORD.toCharArray();
        try {
            store.importProfile("Broken", new ByteArrayInputStream(new byte[]{1, 2, 3}), password, asset("ca.pem"));
            fail("Expected damaged PKCS#12 failure");
        } catch (CertificateProfileException expected) {
            assertEquals(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT, expected.getError());
        }
        assertCleared(password);

        char[] validPassword = PASSWORD.toCharArray();
        try {
            store.importProfile("Broken CA", asset("client.p12"), validPassword,
                    new ByteArrayInputStream("not a certificate".getBytes()));
            fail("Expected damaged CA failure");
        } catch (CertificateProfileException expected) {
            assertEquals(CertificateProfileError.CERTIFICATE_INVALID, expected.getError());
        }
        assertCleared(validPassword);
        assertTrue(store.getProfileIds().isEmpty());
    }

    @Test public void removesClientKeyWhenCaSaveFails() throws Exception {
        int aliasesBefore = clientAliasCount();
        CertificateProfileStore failingStore = new CertificateProfileStore(targetContext,
                new CertificateMaterialReader(), new FailingCaStorage(targetContext));
        char[] password = PASSWORD.toCharArray();
        try {
            failingStore.importProfile("Rollback", asset("client.p12"), password, asset("ca.pem"));
            fail("Expected CA storage failure");
        } catch (CertificateProfileException expected) {
            assertEquals(CertificateProfileError.STORAGE_FAILED, expected.getError());
        }
        assertCleared(password);
        assertTrue(failingStore.getProfileIds().isEmpty());
        assertEquals(aliasesBefore, clientAliasCount());
    }

    @Test public void deletesProfileAndClearsActiveProfile() throws Exception {
        String profileId = importFixture(store, "To delete");
        store.setActiveProfile(profileId);
        store.deleteProfile(profileId);
        assertFalse(store.containsProfile(profileId));
        assertNull(store.getActiveProfileId());
    }

    @Test public void usesSelectedProfileForSslContext() throws Exception {
        String first = importFixture(store, "First");
        String second = importFixture(store, "Second");
        assertFalse(store.getProfile(first).getClientKeyAlias()
                .equals(store.getProfile(second).getClientKeyAlias()));
        store.setActiveProfile(second);
        assertEquals(second, store.getActiveProfile().getProfileId());
        assertNotNull(new ProfileSslContextFactory().create(store, second));
    }

    @Test public void reportsUnavailableUriAndClearsPasswordAcrossObserverRecreation() throws Exception {
        Application application = (Application) targetContext.getApplicationContext();
        CertificateImportViewModel viewModel = new CertificateImportViewModel(application);
        CountDownLatch importing = new CountDownLatch(1);
        Observer<CertificateImportState> firstObserver = state -> {
            if (state.status == CertificateImportState.Status.IMPORTING) importing.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                viewModel.getState().observeForever(firstObserver));

        char[] password = PASSWORD.toCharArray();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> viewModel.importProfile(
                "Unavailable", Uri.parse("content://missing/p12"),
                Uri.parse("content://missing/ca"), password));
        assertTrue(importing.await(5, TimeUnit.SECONDS));

        CountDownLatch error = new CountDownLatch(1);
        Observer<CertificateImportState> recreatedObserver = state -> {
            if (state.status == CertificateImportState.Status.ERROR
                    && state.error == CertificateProfileError.FILE_UNAVAILABLE) error.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            viewModel.getState().removeObserver(firstObserver);
            viewModel.getState().observeForever(recreatedObserver);
        });
        assertTrue(error.await(5, TimeUnit.SECONDS));
        assertCleared(password);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                viewModel.getState().removeObserver(recreatedObserver));
    }

    private String importFixture(CertificateProfileStore profileStore, String name) throws Exception {
        try (InputStream p12 = asset("client.p12"); InputStream ca = asset("ca.pem")) {
            return profileStore.importProfile(name, p12, PASSWORD.toCharArray(), ca);
        }
    }

    private InputStream asset(String name) throws Exception {
        return InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name);
    }

    private static void assertCleared(char[] password) {
        for (char value : password) assertEquals('\0', value);
    }

    private static void clearProfiles(CertificateProfileStore profileStore) {
        for (String id : profileStore.getProfileIds()) profileStore.deleteProfile(id);
    }

    private static int clientAliasCount() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        int count = 0;
        for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
            if (aliases.nextElement().startsWith("mtls_client_")) count++;
        }
        return count;
    }

    private static final class FailingCaStorage extends EncryptedCaStorage {
        FailingCaStorage(Context context) { super(context); }
        @Override void write(String profileId, java.security.cert.X509Certificate caCertificate) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Test CA storage failure");
        }
    }
}
