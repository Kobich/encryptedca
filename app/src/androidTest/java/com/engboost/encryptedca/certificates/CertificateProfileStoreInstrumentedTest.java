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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class CertificateProfileStoreInstrumentedTest {
    private static final String PASSWORD = "1234";
    private Context targetContext;
    private CertificateProfileStore store;

    /**
     * Создаёт хранилище и очищает тестовые профили перед проверкой.
     */
    @Before public void setUp() {
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        store = new CertificateProfileStore(targetContext);
        clearProfiles(store);
    }

    /**
     * Удаляет профили, оставшиеся после проверки.
     */
    @After public void tearDown() {
        clearProfiles(store);
    }

    /**
     * Проверяет импорт и повторное чтение профиля новым экземпляром хранилища.
     *
     * @throws Exception если тестовые assets или Keystore недоступны
     */
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

    /**
     * Проверяет контейнер без пароля и выбранный пользователем self-signed trust anchor без CA:TRUE.
     *
     * @throws Exception если тестовые документы недоступны
     */
    @Test public void importsPkcs12WithoutPasswordAndExplicitTrustAnchor() throws Exception {
        char[] noPassword = new char[0];
        String profileId = store.importProfile("No password", asset("client-no-password.p12"),
                noPassword, asset("ca-without-basic-constraints.pem"));
        assertCleared(noPassword);
        assertEquals("No password", store.getProfile(profileId).getDisplayName());
        assertNotNull(store.getProfile(profileId).getCaCertificate());
    }

    /**
     * Проверяет PBES2/AES-256 контейнер без пароля, созданный современным OpenSSL.
     *
     * @throws Exception если тестовые документы недоступны
     */
    @Test public void importsPasswordlessPbes2Pkcs12() throws Exception {
        char[] noPassword = new char[0];
        String profileId = store.importProfile("PBES2 without password",
                asset("client-no-password-pbes2.p12"), noPassword,
                asset("ca-without-basic-constraints.pem"));
        assertCleared(noPassword);
        assertEquals("PBES2 without password", store.getProfile(profileId).getDisplayName());
    }

    /**
     * Проверяет категорию ошибки пароля, очистку массива и отсутствие записи в индексе.
     *
     * @throws Exception если тестовый asset недоступен
     */
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

    /**
     * Проверяет отказ для повреждённых PKCS#12 и CA и очистку переданных паролей.
     *
     * @throws Exception если тестовые assets недоступны
     */
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

    /**
     * Проверяет удаление ключа из Keystore после ошибки сохранения CA.
     *
     * @throws Exception если тестовые assets или Keystore недоступны
     */
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

    /**
     * Проверяет удаление профиля и сброс активного идентификатора.
     *
     * @throws Exception если тестовые assets недоступны
     */
    @Test public void deletesProfileAndClearsActiveProfile() throws Exception {
        String profileId = importFixture(store, "To delete");
        store.setActiveProfile(profileId);
        store.deleteProfile(profileId);
        assertFalse(store.containsProfile(profileId));
        assertNull(store.getActiveProfileId());
    }

    /**
     * Проверяет разные alias профилей и создание TLS-контекста выбранного профиля.
     *
     * @throws Exception если тестовые assets недоступны
     */
    @Test public void usesSelectedProfileForSslContext() throws Exception {
        String first = importFixture(store, "First");
        String second = importFixture(store, "Second");
        assertFalse(store.getProfile(first).getClientKeyAlias()
                .equals(store.getProfile(second).getClientKeyAlias()));
        store.setActiveProfile(second);
        assertEquals(second, store.getActiveProfile().getProfileId());
        assertNotNull(new ProfileSslContextFactory().create(store, second));
    }

    /**
     * Проверяет ошибку недоступного URI, очистку пароля и получение результата новым наблюдателем.
     *
     * @throws Exception если ожидание состояния превышает тайм-аут
     */
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

    /**
     * Проверяет через тот же ContentResolver и ViewModel, что использует экран, импорт без пароля.
     *
     * @throws Exception если тестовые документы недоступны или импорт не завершился
     */
    @Test public void importsSelectedDocumentsThroughViewModel() throws Exception {
        File p12File = copyAssetToCache("client-no-password.p12");
        File caFile = copyAssetToCache("ca-without-basic-constraints.pem");
        CertificateImportViewModel viewModel = new CertificateImportViewModel(
                (Application) targetContext.getApplicationContext());
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<CertificateImportState> result = new AtomicReference<>();
        Observer<CertificateImportState> observer = state -> {
            if (state.status == CertificateImportState.Status.SUCCESS
                    || state.status == CertificateImportState.Status.ERROR) {
                result.set(state);
                finished.countDown();
            }
        };
        char[] password = new char[0];
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                viewModel.getState().observeForever(observer);
                viewModel.importProfile("No password from picker", Uri.fromFile(p12File),
                        Uri.fromFile(caFile), password);
            });
            assertTrue("Import timed out", finished.await(10, TimeUnit.SECONDS));
            assertEquals("Import error: " + result.get().error,
                    CertificateImportState.Status.SUCCESS, result.get().status);
            assertCleared(password);
            assertNotNull(store.getProfile(result.get().profileId));
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    viewModel.getState().removeObserver(observer));
            p12File.delete();
            caFile.delete();
        }
    }

    /**
     * Импортирует тестовую пару документов и закрывает принадлежащие тесту потоки.
     *
     * @param profileStore хранилище для импорта
     * @param name имя профиля
     * @return идентификатор созданного профиля
     * @throws Exception если asset не удалось открыть или импортировать
     */
    private String importFixture(CertificateProfileStore profileStore, String name) throws Exception {
        try (InputStream p12 = asset("client.p12"); InputStream ca = asset("ca.pem")) {
            return profileStore.importProfile(name, p12, PASSWORD.toCharArray(), ca);
        }
    }

    /**
     * Открывает файл из assets тестового APK.
     *
     * @param name имя файла fixture
     * @return открытый поток, который должен закрыть вызывающий код
     * @throws Exception если asset отсутствует
     */
    private InputStream asset(String name) throws Exception {
        return InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name);
    }

    /**
     * Копирует тестовый документ в доступный приложению каталог для чтения по URI.
     *
     * @param name имя тестового документа
     * @return файл в кэше приложения
     * @throws Exception если документ не удалось скопировать
     */
    private File copyAssetToCache(String name) throws Exception {
        File file = File.createTempFile("certificate-import-", "-" + name, targetContext.getCacheDir());
        try (InputStream input = asset(name); FileOutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
        return file;
    }

    /**
     * Проверяет, что каждый символ массива пароля обнулён.
     *
     * @param password проверяемый массив
     */
    private static void assertCleared(char[] password) {
        for (char value : password) assertEquals('\0', value);
    }

    /**
     * Удаляет все профили тестового хранилища.
     *
     * @param profileStore очищаемое хранилище
     */
    private static void clearProfiles(CertificateProfileStore profileStore) {
        for (String id : profileStore.getProfileIds()) profileStore.deleteProfile(id);
    }

    /**
     * Считает клиентские alias в Android Keystore.
     *
     * @return число alias с префиксом mtls_client_
     * @throws Exception если Keystore недоступен
     */
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
        /**
         * Создаёт тестовое хранилище, имитирующее отказ записи CA.
         *
         * @param context контекст приложения
         */
        FailingCaStorage(Context context) { super(context); }

        /**
         * Завершает запись предсказуемой ошибкой для проверки отката импорта.
         *
         * @param profileId идентификатор профиля
         * @param caCertificate сертификат CA
         * @throws CertificateProfileException всегда сообщает тестовую ошибку хранения
         */
        @Override void write(String profileId, java.security.cert.X509Certificate caCertificate) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Test CA storage failure");
        }
    }
}
