package com.engboost.encryptedca.certificates;

import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

/** Builds an SSLContext whose client authentication is pinned to one profile alias. */
public final class ProfileSslContextFactory {
    public SSLContext create(CertificateProfileStore store, String profileId) {
        if (store == null) throw new IllegalArgumentException("CertificateProfileStore is required");
        try {
            ActiveCertificateProfile profile = store.getProfile(profileId);

            KeyStore clientStore = KeyStore.getInstance("AndroidKeyStore");
            clientStore.load(null);
            if (!clientStore.isKeyEntry(profile.getClientKeyAlias())) {
                throw new CertificateProfileException("Client key does not exist for " + profileId);
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
            throw new CertificateProfileException("Could not create SSLContext for " + profileId, e);
        }
    }

    private static X509ExtendedKeyManager findX509ExtendedKeyManager(KeyManager[] managers) {
        for (KeyManager manager : managers) {
            if (manager instanceof X509ExtendedKeyManager) return (X509ExtendedKeyManager) manager;
        }
        throw new CertificateProfileException("No X509ExtendedKeyManager was created");
    }

    private static KeyManager[] replaceKeyManager(KeyManager[] managers, X509ExtendedKeyManager replacement) {
        KeyManager[] result = managers.clone();
        for (int i = 0; i < result.length; i++) {
            if (result[i] instanceof X509ExtendedKeyManager) {
                result[i] = replacement;
                return result;
            }
        }
        throw new CertificateProfileException("No X509ExtendedKeyManager was created");
    }
}
