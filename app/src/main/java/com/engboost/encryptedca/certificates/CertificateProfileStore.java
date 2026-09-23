package com.engboost.encryptedca.certificates;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Isolated persistence for mTLS profiles. It never persists a PKCS#12 password,
 * source PKCS#12 file, or plaintext CA PEM.
 */
public final class CertificateProfileStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String PREFS = "certificate_profiles";
    private static final String IDS = "profile_ids";
    private static final String ACTIVE_ID = "active_profile_id";
    private static final String DISPLAY_NAME_PREFIX = "display_name_";
    private static final String CLIENT_ALIAS_PREFIX = "mtls_client_";
    private static final String CA_KEY_ALIAS = "mtls_ca_storage_key";
    private static final int GCM_IV_BYTES = 12;

    private final Context context;
    private final SharedPreferences preferences;

    public CertificateProfileStore(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String importProfile(String displayName, InputStream p12InputStream,
                                char[] p12Password, InputStream caPemInputStream) {
        if (p12InputStream == null || p12Password == null || caPemInputStream == null) {
            throw new IllegalArgumentException("PKCS#12, password and CA PEM are required");
        }
        String profileId = null;
        boolean keyImported = false;
        boolean caWritten = false;
        try {
            KeyStore.PrivateKeyEntry entry = readPrivateKeyEntry(p12InputStream, p12Password);
            PrivateKey privateKey = entry.getPrivateKey();
            Certificate[] chain = entry.getCertificateChain();
            if (privateKey == null) {
                throw new CertificateProfileException("PKCS#12 does not contain a private key");
            }
            if (chain == null || chain.length == 0) {
                throw new CertificateProfileException("PKCS#12 contains an empty certificate chain");
            }
            if (!(chain[0] instanceof X509Certificate)) {
                throw new CertificateProfileException("Client certificate is not X.509");
            }
            ((X509Certificate) chain[0]).checkValidity();

            X509Certificate caCertificate = readCaCertificate(caPemInputStream);
            profileId = UUID.randomUUID().toString();
            if (containsProfile(profileId)) {
                throw new CertificateProfileException("Generated profileId already exists: " + profileId);
            }

            importClientEntry(clientKeyAlias(profileId), privateKey, chain);
            keyImported = true;
            writeEncryptedCa(profileId, caCertificate.getEncoded());
            caWritten = true;

            Set<String> ids = new LinkedHashSet<>(profileIds());
            ids.add(profileId);
            if (!preferences.edit()
                    .putStringSet(IDS, ids)
                    .putString(DISPLAY_NAME_PREFIX + profileId, displayName)
                    .commit()) {
                throw new CertificateProfileException("Could not persist certificate profile index");
            }
            return profileId;
        } catch (java.security.UnrecoverableKeyException e) {
            throw new CertificateProfileException("Invalid PKCS#12 password", e);
        } catch (IOException e) {
            // PKCS#12 providers commonly report a wrong password as IOException.
            String message = e.getMessage();
            if (looksLikePasswordError(message)) {
                throw new CertificateProfileException("Invalid PKCS#12 password", e);
            }
            rollback(profileId, keyImported, caWritten);
            throw new CertificateProfileException("Could not import certificate profile", e);
        } catch (CertificateProfileException e) {
            rollback(profileId, keyImported, caWritten);
            throw e;
        } catch (Exception e) {
            rollback(profileId, keyImported, caWritten);
            throw new CertificateProfileException("Could not import certificate profile", e);
        } finally {
            Arrays.fill(p12Password, '\0');
        }
    }

    public List<String> getProfileIds() {
        return Collections.unmodifiableList(new ArrayList<>(profileIds()));
    }

    public void setActiveProfile(String profileId) {
        if (!containsProfile(profileId)) {
            throw new IllegalArgumentException("Unknown profileId: " + profileId);
        }
        if (!preferences.edit().putString(ACTIVE_ID, profileId).commit()) {
            throw new CertificateProfileException("Could not save active profile");
        }
    }

    public String getActiveProfileId() { return preferences.getString(ACTIVE_ID, null); }

    public ActiveCertificateProfile getActiveProfile() {
        String profileId = getActiveProfileId();
        return profileId == null ? null : getProfile(profileId);
    }

    public void deleteProfile(String profileId) {
        if (!containsProfile(profileId)) return;
        try {
            KeyStore keyStore = androidKeyStore();
            String alias = clientKeyAlias(profileId);
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias);
        } catch (Exception e) {
            throw new CertificateProfileException("Could not delete client key", e);
        }
        File caFile = caFile(profileId);
        if (caFile.exists() && !caFile.delete()) {
            throw new CertificateProfileException("Could not delete encrypted CA file");
        }
        Set<String> ids = new LinkedHashSet<>(profileIds());
        ids.remove(profileId);
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet(IDS, ids)
                .remove(DISPLAY_NAME_PREFIX + profileId);
        if (profileId.equals(getActiveProfileId())) editor.remove(ACTIVE_ID);
        if (!editor.commit()) throw new CertificateProfileException("Could not update certificate profile index");
    }

    public boolean containsProfile(String profileId) {
        return profileId != null && profileIds().contains(profileId);
    }

    ActiveCertificateProfile getProfile(String profileId) {
        if (!containsProfile(profileId)) throw new IllegalArgumentException("Unknown profileId: " + profileId);
        try {
            KeyStore keyStore = androidKeyStore();
            if (!keyStore.isKeyEntry(clientKeyAlias(profileId)) || !caFile(profileId).isFile()) {
                throw new CertificateProfileException(
                        "Certificate profile is incomplete. Import it again: " + profileId);
            }
        } catch (CertificateProfileException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateProfileException("Could not inspect profile " + profileId, e);
        }
        return new ActiveCertificateProfile(profileId,
                preferences.getString(DISPLAY_NAME_PREFIX + profileId, null),
                clientKeyAlias(profileId), readEncryptedCa(profileId));
    }

    static String clientKeyAlias(String profileId) { return CLIENT_ALIAS_PREFIX + profileId; }

    private KeyStore.PrivateKeyEntry readPrivateKeyEntry(InputStream input, char[] password) throws Exception {
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(input, password);
        for (java.util.Enumeration<String> aliases = pkcs12.aliases(); aliases.hasMoreElements();) {
            String alias = aliases.nextElement();
            if (pkcs12.isKeyEntry(alias)) {
                KeyStore.Entry entry = pkcs12.getEntry(alias, new KeyStore.PasswordProtection(password));
                if (entry instanceof KeyStore.PrivateKeyEntry) return (KeyStore.PrivateKeyEntry) entry;
            }
        }
        throw new CertificateProfileException("PKCS#12 does not contain a PrivateKeyEntry");
    }

    private X509Certificate readCaCertificate(InputStream pemInput) throws CertificateException {
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(pemInput);
        if (!(certificate instanceof X509Certificate)) throw new CertificateProfileException("CA certificate is not X.509");
        X509Certificate ca = (X509Certificate) certificate;
        ca.checkValidity();
        if (ca.getBasicConstraints() < 0) throw new CertificateProfileException("PEM certificate is not a CA certificate");
        return ca;
    }

    private void importClientEntry(String alias, PrivateKey key, Certificate[] chain) throws Exception {
        KeyProtection.Builder builder = new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
        if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                    KeyProperties.SIGNATURE_PADDING_RSA_PSS);
        }
        KeyProtection protection = builder.build();
        androidKeyStore().setEntry(alias, new KeyStore.PrivateKeyEntry(key, chain), protection);
    }

    private void writeEncryptedCa(String profileId, byte[] certificateDer) throws Exception {
        SecretKey key = caEncryptionKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        // The Android Keystore creates a fresh IV for a randomized-encryption key.
        // Supplying an IV ourselves is rejected while randomized encryption is required.
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length != GCM_IV_BYTES) {
            throw new IOException("Android Keystore did not provide a valid GCM IV");
        }
        byte[] encrypted = cipher.doFinal(certificateDer);
        File target = caFile(profileId);
        File temp = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(iv.length);
            output.write(iv);
            output.write(encrypted);
            output.getFD().sync();
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IOException("Could not move encrypted CA into place");
        }
    }

    private X509Certificate readEncryptedCa(String profileId) {
        try (FileInputStream input = new FileInputStream(caFile(profileId))) {
            int ivLength = input.read();
            if (ivLength < 12 || ivLength > 16) throw new IOException("Invalid encrypted CA IV");
            byte[] iv = readFully(input, ivLength);
            byte[] encrypted = readFully(input, (int) caFile(profileId).length() - 1 - ivLength);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, caEncryptionKey(), new GCMParameterSpec(128, iv));
            byte[] der = cipher.doFinal(encrypted);
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
            certificate.checkValidity();
            return certificate;
        } catch (Exception e) {
            throw new CertificateProfileException("Could not read encrypted CA for " + profileId, e);
        }
    }

    private SecretKey caEncryptionKey() throws Exception {
        KeyStore store = androidKeyStore();
        Key key = store.getKey(CA_KEY_ALIAS, null);
        if (key instanceof SecretKey) return (SecretKey) key;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(CA_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private KeyStore androidKeyStore() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
        return store;
    }

    private Set<String> profileIds() {
        Set<String> result = preferences.getStringSet(IDS, Collections.emptySet());
        return result == null ? Collections.emptySet() : new LinkedHashSet<>(result);
    }

    private File caFile(String profileId) { return new File(context.getNoBackupFilesDir(), profileId + ".ca.enc"); }

    private static byte[] readFully(InputStream input, int length) throws IOException {
        if (length < 0) throw new IOException("Invalid encrypted CA length");
        ByteArrayOutputStream output = new ByteArrayOutputStream(length);
        byte[] buffer = new byte[4096];
        while (output.size() < length) {
            int read = input.read(buffer, 0, Math.min(buffer.length, length - output.size()));
            if (read < 0) throw new IOException("Unexpected end of encrypted CA");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static boolean looksLikePasswordError(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase(java.util.Locale.US);
        return normalized.contains("password")
                || normalized.contains("integrity check")
                || normalized.contains("mac invalid")
                || normalized.contains("decrypt");
    }

    private void rollback(String profileId, boolean keyImported, boolean caWritten) {
        if (profileId == null) return;
        if (keyImported) {
            try { androidKeyStore().deleteEntry(clientKeyAlias(profileId)); } catch (Exception ignored) { }
        }
        if (caWritten) caFile(profileId).delete();
    }
}
