package com.engboost.encryptedca.certificates;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Координирует импорт профиля и работу с его индексом. Потоки остаются у вызывающего кода. */
public final class CertificateProfileStore {
    private static final String PREFS = "certificate_profiles";
    private static final String IDS = "profile_ids";
    private static final String ACTIVE_ID = "active_profile_id";
    private static final String DISPLAY_NAME_PREFIX = "display_name_";
    private static final String CLIENT_ALIAS_PREFIX = "mtls_client_";

    private final SharedPreferences preferences;
    private final CertificateMaterialReader materialReader;
    private final EncryptedCaStorage caStorage;

    /**
     * Создаёт хранилище профилей для контекста приложения.
     *
     * @param context контекст Android-приложения
     */
    public CertificateProfileStore(Context context) {
        this(context, new CertificateMaterialReader(), new EncryptedCaStorage(context));
    }

    /**
     * Создаёт хранилище с указанными компонентами обработки сертификатов.
     *
     * @param context контекст приложения
     * @param materialReader компонент чтения и проверки сертификатов
     * @param caStorage компонент зашифрованного хранения CA
     */
    CertificateProfileStore(Context context, CertificateMaterialReader materialReader,
                            EncryptedCaStorage caStorage) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.materialReader = materialReader;
        this.caStorage = caStorage;
    }

    /**
     * Импортирует профиль и очищает переданный пароль при любом исходе.
     * Откат компенсирующий: он не является общей транзакцией Keystore, файлов и Preferences.
     *
     * @param displayName необязательное имя профиля
     * @param p12InputStream поток PKCS#12; остаётся во владении вызывающего кода
     * @param p12Password пароль PKCS#12; владение передаётся методу и массив очищается
     * @param caPemInputStream поток сертификата CA; остаётся во владении вызывающего кода
     * @return идентификатор созданного профиля
     * @throws IllegalArgumentException если обязательный поток или пароль равен null
     * @throws CertificateProfileException если сертификаты не прошли проверку или сохранение завершилось ошибкой
     */
    public String importProfile(String displayName, InputStream p12InputStream,
                                char[] p12Password, InputStream caPemInputStream) {
        if (p12InputStream == null || p12Password == null || caPemInputStream == null) {
            throw new IllegalArgumentException("PKCS#12, password and CA PEM are required");
        }
        String profileId = null;
        boolean clientKeySaved = false;
        boolean caSaveAttempted = false;
        try {
            ClientKeyMaterial client = materialReader.readPkcs12(p12InputStream, p12Password);
            X509Certificate caCertificate = materialReader.readCaPem(caPemInputStream);
            profileId = UUID.randomUUID().toString();
            if (containsProfile(profileId)) {
                throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                        "Generated profileId already exists");
            }
            saveClientKey(profileId, client);
            clientKeySaved = true;
            caSaveAttempted = true;
            caStorage.write(profileId, caCertificate);
            registerProfile(profileId, displayName);
            return profileId;
        } catch (CertificateProfileException e) {
            rollbackImport(profileId, clientKeySaved, caSaveAttempted, e);
            throw e;
        } catch (Exception e) {
            CertificateProfileException failure = new CertificateProfileException(
                    CertificateProfileError.STORAGE_FAILED, "Could not import certificate profile", e);
            rollbackImport(profileId, clientKeySaved, caSaveAttempted, failure);
            throw failure;
        } finally {
            Arrays.fill(p12Password, '\0');
        }
    }

    /**
     * Возвращает идентификаторы зарегистрированных профилей.
     *
     * @return неизменяемый список profileId
     */
    public List<String> getProfileIds() {
        return Collections.unmodifiableList(new ArrayList<>(profileIds()));
    }

    /**
     * Сохраняет выбранный активный профиль.
     *
     * @param profileId идентификатор существующего профиля
     * @throws CertificateProfileException если профиль отсутствует или индекс не удалось сохранить
     */
    public void setActiveProfile(String profileId) {
        if (!containsProfile(profileId)) {
            throw new CertificateProfileException(CertificateProfileError.PROFILE_INCOMPLETE,
                    "Unknown profileId");
        }
        if (!preferences.edit().putString(ACTIVE_ID, profileId).commit()) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not save active profile");
        }
    }

    /**
     * Возвращает идентификатор активного профиля.
     *
     * @return profileId либо null, если профиль не выбран
     */
    public String getActiveProfileId() {
        return preferences.getString(ACTIVE_ID, null);
    }

    /**
     * Читает данные активного профиля.
     *
     * @return профиль или null, если активный профиль не задан
     * @throws CertificateProfileException если данные активного профиля неполны или повреждены
     */
    public CertificateProfile getActiveProfile() {
        String profileId = getActiveProfileId();
        return profileId == null ? null : getProfile(profileId);
    }

    /**
     * Удаляет ключ, CA-файл и запись профиля; при удалении активного профиля сбрасывает выбор.
     *
     * @param profileId идентификатор удаляемого профиля
     * @throws CertificateProfileException если один из этапов удаления завершился ошибкой
     */
    public void deleteProfile(String profileId) {
        if (!containsProfile(profileId)) {
            return;
        }
        CertificateProfileException failure = null;
        try {
            deleteClientKey(profileId);
        } catch (CertificateProfileException e) {
            failure = e;
        }
        try {
            caStorage.delete(profileId);
        } catch (CertificateProfileException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
        Set<String> ids = new LinkedHashSet<>(profileIds());
        ids.remove(profileId);
        SharedPreferences.Editor editor = preferences.edit().putStringSet(IDS, ids)
                .remove(DISPLAY_NAME_PREFIX + profileId);
        if (profileId.equals(getActiveProfileId())) {
            editor.remove(ACTIVE_ID);
        }
        if (!editor.commit()) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not update certificate profile index");
        }
    }

    /**
     * Проверяет наличие profileId в индексе.
     *
     * @param profileId проверяемый идентификатор
     * @return true, если профиль зарегистрирован
     */
    public boolean containsProfile(String profileId) {
        return profileId != null && profileIds().contains(profileId);
    }

    /**
     * Читает существующий профиль и проверяет наличие ключа и CA-файла.
     *
     * @param profileId идентификатор профиля
     * @return сохранённые данные профиля
     * @throws CertificateProfileException если профиль отсутствует, неполон или повреждён
     */
    CertificateProfile getProfile(String profileId) {
        if (!containsProfile(profileId)) {
            throw new CertificateProfileException(CertificateProfileError.PROFILE_INCOMPLETE,
                    "Unknown profileId");
        }
        if (!hasClientKey(profileId) || !caStorage.exists(profileId)) {
            throw new CertificateProfileException(CertificateProfileError.PROFILE_INCOMPLETE,
                    "Certificate profile is incomplete");
        }
        return new CertificateProfile(profileId,
                preferences.getString(DISPLAY_NAME_PREFIX + profileId, null),
                clientKeyAlias(profileId), caStorage.read(profileId));
    }

    /**
     * Формирует совместимый alias клиентского ключа.
     *
     * @param profileId идентификатор профиля
     * @return alias в формате mtls_client_<profileId>
     */
    static String clientKeyAlias(String profileId) {
        return CLIENT_ALIAS_PREFIX + profileId;
    }

    /**
     * Сохраняет закрытый ключ и цепочку сертификатов в Android Keystore.
     *
     * @param profileId идентификатор профиля
     * @param client проверенный комплект ключа и цепочки
     * @throws CertificateProfileException если ключ не удалось сохранить
     */
    private void saveClientKey(String profileId, ClientKeyMaterial client) {
        try {
            KeyStore store = androidKeyStore();
            String alias = clientKeyAlias(profileId);
            if (store.containsAlias(alias)) {
                throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                        "Client key alias exists");
            }
            KeyProtection.Builder protection = new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
                    .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256,
                            KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
            if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(client.privateKey.getAlgorithm())) {
                protection.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                        KeyProperties.SIGNATURE_PADDING_RSA_PSS);
            }
            store.setEntry(alias, new KeyStore.PrivateKeyEntry(client.privateKey, client.certificateChain),
                    protection.build());
        } catch (CertificateProfileException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not save client key", e);
        }
    }

    /**
     * Добавляет профиль и его отображаемое имя в индекс Preferences.
     *
     * @param profileId идентификатор сохраняемого профиля
     * @param displayName необязательное имя профиля
     * @throws CertificateProfileException если индекс не удалось записать
     */
    private void registerProfile(String profileId, String displayName) {
        Set<String> ids = new LinkedHashSet<>(profileIds());
        ids.add(profileId);
        if (!preferences.edit().putStringSet(IDS, ids)
                .putString(DISPLAY_NAME_PREFIX + profileId, displayName).commit()) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not persist certificate profile index");
        }
    }

    /**
     * Проверяет наличие существующей записи ключа в Android Keystore.
     *
     * @param profileId идентификатор профиля
     * @return true, если alias содержит ключ
     * @throws CertificateProfileException если Keystore недоступен
     */
    private boolean hasClientKey(String profileId) {
        try {
            return androidKeyStore().isKeyEntry(clientKeyAlias(profileId));
        } catch (Exception e) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not inspect client key", e);
        }
    }

    /**
     * Удаляет ключ и связанную с ним цепочку из Android Keystore.
     *
     * @param profileId идентификатор профиля
     * @throws CertificateProfileException если запись не удалось удалить
     */
    private void deleteClientKey(String profileId) {
        try {
            KeyStore store = androidKeyStore();
            if (store.containsAlias(clientKeyAlias(profileId))) {
                store.deleteEntry(clientKeyAlias(profileId));
            }
        } catch (Exception e) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not delete client key", e);
        }
    }

    /**
     * Выполняет компенсирующую очистку ресурсов после неудачного импорта.
     *
     * @param profileId идентификатор незавершённого профиля
     * @param clientKeySaved был ли сохранён клиентский ключ
     * @param caSaveAttempted начиналась ли запись CA-файла
     * @param original исходная ошибка импорта, к которой добавляются ошибки очистки
     */
    private void rollbackImport(String profileId, boolean clientKeySaved, boolean caSaveAttempted,
                                CertificateProfileException original) {
        if (profileId == null) {
            return;
        }
        try {
            unregisterProfile(profileId);
        } catch (Exception cleanupError) {
            original.addSuppressed(cleanupError);
        }
        if (caSaveAttempted) {
            try {
                caStorage.delete(profileId);
            } catch (Exception cleanupError) {
                original.addSuppressed(cleanupError);
            }
        }
        if (clientKeySaved) {
            try {
                deleteClientKey(profileId);
            } catch (Exception cleanupError) {
                original.addSuppressed(cleanupError);
            }
        }
    }

    /**
     * Убирает профиль из индекса, если регистрация уже успела завершиться.
     *
     * @param profileId идентификатор профиля
     * @throws CertificateProfileException если индекс не удалось обновить
     */
    private void unregisterProfile(String profileId) {
        Set<String> ids = new LinkedHashSet<>(profileIds());
        if (!ids.remove(profileId)) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit().putStringSet(IDS, ids)
                .remove(DISPLAY_NAME_PREFIX + profileId);
        if (profileId.equals(getActiveProfileId())) {
            editor.remove(ACTIVE_ID);
        }
        if (!editor.commit()) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not roll back certificate profile index");
        }
    }

    /**
     * Открывает Android Keystore.
     *
     * @return загруженное хранилище ключей устройства
     * @throws Exception если провайдер Keystore недоступен
     */
    private static KeyStore androidKeyStore() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        return store;
    }

    /**
     * Копирует множество идентификаторов из Preferences.
     *
     * @return изменяемая копия сохранённого набора идентификаторов
     */
    private Set<String> profileIds() {
        Set<String> stored = preferences.getStringSet(IDS, Collections.emptySet());
        return stored == null ? Collections.emptySet() : new LinkedHashSet<>(stored);
    }
}
