package com.engboost.encryptedca.certificates;

import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

/** Создаёт TLS-контекст с клиентской аутентификацией через alias выбранного профиля. */
public final class ProfileSslContextFactory {
    /**
     * Создаёт TLS-контекст с ключом и CA заданного профиля.
     *
     * @param store хранилище профилей
     * @param profileId идентификатор выбранного профиля
     * @return TLS-контекст с выбранным клиентским alias и CA
     * @throws CertificateProfileException если профиль или компоненты TLS недоступны
     */
    public SSLContext create(CertificateProfileStore store, String profileId) {
        if (store == null) {
            throw new IllegalArgumentException("CertificateProfileStore is required");
        }
        try {
            CertificateProfile profile = store.getProfile(profileId);

            KeyStore clientStore = KeyStore.getInstance("AndroidKeyStore");
            clientStore.load(null);
            if (!clientStore.isKeyEntry(profile.getClientKeyAlias())) {
                throw new CertificateProfileException(CertificateProfileError.PROFILE_INCOMPLETE,
                        "Client key does not exist");
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientStore, null);

            X509ExtendedKeyManager keyManager = findX509ExtendedKeyManager(kmf.getKeyManagers());
            KeyManager[] selectedManagers = replaceKeyManager(kmf.getKeyManagers(),
                    new SelectedAliasKeyManager(keyManager, profile.getClientKeyAlias()));

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);
            trustStore.setCertificateEntry("profile_ca", profile.getCaCertificate());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(selectedManagers, tmf.getTrustManagers(), null);
            return context;
        } catch (CertificateProfileException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not create SSLContext", e);
        }
    }

    /**
     * Находит X509-менеджер среди созданных KeyManager.
     *
     * @param managers менеджеры ключей
     * @return найденный X509ExtendedKeyManager
     * @throws CertificateProfileException если такого менеджера нет
     */
    private static X509ExtendedKeyManager findX509ExtendedKeyManager(KeyManager[] managers) {
        for (KeyManager manager : managers) {
            if (manager instanceof X509ExtendedKeyManager) return (X509ExtendedKeyManager) manager;
        }
        throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                "No X509ExtendedKeyManager was created");
    }

    /**
     * Заменяет клиентский X509-менеджер менеджером выбранного alias.
     *
     * @param managers исходный набор менеджеров
     * @param replacement менеджер с ограничением на alias
     * @return копия набора с заменённым менеджером
     * @throws CertificateProfileException если заменяемый менеджер не найден
     */
    private static KeyManager[] replaceKeyManager(KeyManager[] managers, X509ExtendedKeyManager replacement) {
        KeyManager[] result = managers.clone();
        for (int i = 0; i < result.length; i++) {
            if (result[i] instanceof X509ExtendedKeyManager) {
                result[i] = replacement;
                return result;
            }
        }
        throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                "No X509ExtendedKeyManager was created");
    }
}
