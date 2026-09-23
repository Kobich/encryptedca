package com.engboost.encryptedca.certificates;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Хранит DER сертификата CA в совместимом формате [длина IV][IV][AES-GCM шифротекст]. */
class EncryptedCaStorage {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String CA_KEY_ALIAS = "mtls_ca_storage_key";

    private final Context context;

    /**
     * Создаёт CA-хранилище в каталоге приложения, исключённом из backup.
     *
     * @param context контекст Android-приложения
     */
    EncryptedCaStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Шифрует сертификат новым Keystore IV и сохраняет его в совместимом формате файла.
     *
     * @param profileId идентификатор профиля
     * @param caCertificate проверенный сертификат CA
     * @throws CertificateProfileException если шифрование, запись или перенос файла завершились ошибкой
     */
    void write(String profileId, X509Certificate caCertificate) {
        File temp = temporaryFile(profileId);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateEncryptionKey());
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length < 12 || iv.length > 16) {
                throw new IOException("Invalid GCM IV from Android Keystore");
            }
            byte[] encrypted = cipher.doFinal(caCertificate.getEncoded());
            try (FileOutputStream output = new FileOutputStream(temp)) {
                output.write(iv.length);
                output.write(iv);
                output.write(encrypted);
                output.getFD().sync();
            }
            if (!temp.renameTo(caFile(profileId))) {
                throw new IOException("Could not move encrypted CA into place");
            }
        } catch (Exception e) {
            CertificateProfileException failure = new CertificateProfileException(
                    CertificateProfileError.STORAGE_FAILED, "Could not save encrypted CA", e);
            deleteTemporary(profileId, failure);
            throw failure;
        }
    }

    /**
     * Читает существующий CA-файл и расшифровывает его существующим Keystore-ключом.
     *
     * @param profileId идентификатор профиля
     * @return сертификат CA
     * @throws CertificateProfileException если файл или ключ отсутствует, повреждён либо не читается
     */
    X509Certificate read(String profileId) {
        File source = caFile(profileId);
        try (FileInputStream input = new FileInputStream(source)) {
            int ivLength = input.read();
            if (ivLength < 12 || ivLength > 16) throw new IOException("Invalid encrypted CA IV");
            byte[] iv = readFully(input, ivLength);
            byte[] encrypted = readFully(input, (int) source.length() - ivLength - 1);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getExistingEncryptionKey(), new GCMParameterSpec(128, iv));
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(cipher.doFinal(encrypted)));
            certificate.checkValidity();
            return certificate;
        } catch (CertificateProfileException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateProfileException(CertificateProfileError.STORAGE_FAILED,
                    "Could not read encrypted CA", e);
        }
    }

    /**
     * Проверяет наличие сохранённого CA-файла профиля.
     *
     * @param profileId идентификатор профиля
     * @return true, если файл существует
     */
    boolean exists(String profileId) { return caFile(profileId).isFile(); }

    /**
     * Удаляет CA-файл и оставшийся временный файл профиля.
     *
     * @param profileId идентификатор профиля
     * @throws CertificateProfileException если файл не удалось удалить
     */
    void delete(String profileId) {
        CertificateProfileException failure = null;
        failure = deleteFile(caFile(profileId), failure);
        failure = deleteFile(temporaryFile(profileId), failure);
        if (failure != null) throw failure;
    }

    /**
     * Получает уже существующий ключ шифрования CA.
     *
     * @return ключ AES из Android Keystore
     * @throws Exception если Keystore недоступен
     * @throws CertificateProfileException если ключ отсутствует
     */
    private SecretKey getExistingEncryptionKey() throws Exception {
        Key key = androidKeyStore().getKey(CA_KEY_ALIAS, null);
        if (!(key instanceof SecretKey)) {
            throw new CertificateProfileException(CertificateProfileError.PROFILE_INCOMPLETE,
                    "CA encryption key is missing");
        }
        return (SecretKey) key;
    }

    /**
     * Получает ключ CA или создаёт его при первом импорте.
     *
     * @return ключ AES из Android Keystore
     * @throws Exception если ключ не удалось получить или создать
     */
    private SecretKey getOrCreateEncryptionKey() throws Exception {
        try {
            return getExistingEncryptionKey();
        } catch (CertificateProfileException e) {
            if (e.getError() != CertificateProfileError.PROFILE_INCOMPLETE) throw e;
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(CA_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    /**
     * Открывает Android Keystore.
     *
     * @return загруженное хранилище ключей устройства
     * @throws Exception если провайдер Keystore недоступен
     */
    private static KeyStore androidKeyStore() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
        return store;
    }

    /**
     * Формирует путь CA-файла в сохранённом формате.
     *
     * @param profileId идентификатор профиля
     * @return файл <profileId>.ca.enc
     */
    private File caFile(String profileId) { return new File(context.getNoBackupFilesDir(), profileId + ".ca.enc"); }

    /**
     * Формирует путь временного файла для записи CA.
     *
     * @param profileId идентификатор профиля
     * @return временный файл <profileId>.ca.enc.tmp
     */
    private File temporaryFile(String profileId) { return new File(context.getNoBackupFilesDir(), profileId + ".ca.enc.tmp"); }

    /**
     * Удаляет временный файл и прикрепляет ошибку очистки к исходной ошибке записи.
     *
     * @param profileId идентификатор профиля
     * @param original исходная ошибка записи
     */
    private void deleteTemporary(String profileId, CertificateProfileException original) {
        try { delete(temporaryFile(profileId)); } catch (Exception cleanupError) { original.addSuppressed(cleanupError); }
    }
    /**
     * Удаляет файл, если он существует.
     *
     * @param file удаляемый файл
     * @throws IOException если файл не удалось удалить
     */
    private static void delete(File file) throws IOException {
        if (file.exists() && !file.delete()) throw new IOException("Could not delete " + file.getName());
    }
    /**
     * Удаляет файл и сохраняет ошибку очистки в имеющейся ошибке.
     *
     * @param file удаляемый файл
     * @param prior ранее возникшая ошибка или null
     * @return исходная либо созданная ошибка очистки
     */
    private static CertificateProfileException deleteFile(File file, CertificateProfileException prior) {
        try { delete(file); return prior; } catch (Exception e) {
            CertificateProfileException failure = prior == null ? new CertificateProfileException(
                    CertificateProfileError.STORAGE_FAILED, "Could not delete encrypted CA", e) : prior;
            if (prior != null) prior.addSuppressed(e);
            return failure;
        }
    }
    /**
     * Читает заданное количество байтов из потока.
     *
     * @param input входной поток
     * @param length ожидаемое число байтов
     * @return прочитанные байты
     * @throws IOException если длина некорректна или поток завершился раньше
     */
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
}
