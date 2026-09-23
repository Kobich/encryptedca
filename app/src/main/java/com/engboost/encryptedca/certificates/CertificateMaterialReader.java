package com.engboost.encryptedca.certificates;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Arrays;

/** Читает и проверяет сертификаты; входные потоки остаются у вызывающего кода. */
final class CertificateMaterialReader {
    /**
     * Читает PKCS#12, находит закрытый ключ и проверяет клиентскую цепочку.
     *
     * @param input поток PKCS#12; остаётся во владении вызывающего кода
     * @param password пароль контейнера
     * @return проверенный закрытый ключ и цепочка сертификатов
     * @throws CertificateProfileException если контейнер не открылся, ключ не найден или сертификат непригоден
     */
    ClientKeyMaterial readPkcs12(InputStream input, char[] password) {
        byte[] encoded = null;
        try {
            encoded = readAll(input);
            if (password.length != 0) {
                return readPkcs12(encoded, password);
            }
            return readWithoutPassword(encoded);
        } catch (CertificateProfileException e) {
            throw e;
        } catch (IOException e) {
            throw invalidContainer(e);
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    /**
     * Открывает контейнер без пароля с null, пустым массивом и одним нулевым символом.
     * Android PKCS#12-провайдеры и экспортёры по-разному трактуют эти варианты.
     *
     * @param encoded содержимое PKCS#12 только в оперативной памяти
     * @return проверенный закрытый ключ и цепочка сертификатов
     * @throws CertificateProfileException если оба способа не открыли контейнер
     */
    private ClientKeyMaterial readWithoutPassword(byte[] encoded) {
        try {
            return readPkcs12(encoded, null);
        } catch (CertificateProfileException withoutPassword) {
            if (withoutPassword.getError() != CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT) {
                throw withoutPassword;
            }
            try {
                return readPkcs12(encoded, new char[0]);
            } catch (CertificateProfileException emptyPassword) {
                char[] zeroCharacterPassword = new char[]{'\0'};
                try {
                    return readPkcs12(encoded, zeroCharacterPassword);
                } catch (CertificateProfileException zeroCharacterFailure) {
                    zeroCharacterFailure.addSuppressed(emptyPassword);
                    zeroCharacterFailure.addSuppressed(withoutPassword);
                    throw zeroCharacterFailure;
                } finally {
                    Arrays.fill(zeroCharacterPassword, '\0');
                }
            }
        }
    }

    /**
     * Открывает контейнер заданным представлением пароля и извлекает PrivateKeyEntry.
     *
     * @param encoded содержимое PKCS#12 только в оперативной памяти
     * @param password пароль либо null для контейнера без пароля
     * @return проверенный закрытый ключ и цепочка сертификатов
     * @throws CertificateProfileException если контейнер или ключ не удалось прочитать
     */
    private ClientKeyMaterial readPkcs12(byte[] encoded, char[] password) {
        KeyStore pkcs12;
        try {
            pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(new java.io.ByteArrayInputStream(encoded), password);
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            // Провайдеры сообщают одну и ту же ошибку для пароля, формата и повреждённого контейнера.
            throw invalidContainer(e);
        }
        try {
            for (Enumeration<String> aliases = pkcs12.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                if (!pkcs12.isKeyEntry(alias)) {
                    continue;
                }
                ProtectionParameter protection = password == null ? null
                        : new KeyStore.PasswordProtection(password);
                KeyStore.Entry entry = pkcs12.getEntry(alias, protection);
                if (entry instanceof KeyStore.PrivateKeyEntry) {
                    return validateClientEntry((KeyStore.PrivateKeyEntry) entry);
                }
            }
            throw unavailableKey(null);
        } catch (CertificateProfileException e) {
            throw e;
        } catch (Exception e) {
            throw unavailableKey(e);
        }
    }

    /**
     * Копирует PKCS#12 в память, чтобы без пароля попробовать варианты кодировки.
     *
     * @param input поток PKCS#12; остаётся во владении вызывающего кода
     * @return байты контейнера, которые вызывающий метод очищает после чтения
     * @throws IOException если поток не удалось прочитать
     */
    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        Arrays.fill(buffer, (byte) 0);
        return output.toByteArray();
    }

    /**
     * Разбирает CA PEM и проверяет тип и срок действия сертификата доверия.
     *
     * @param input поток PEM; остаётся во владении вызывающего кода
     * @return проверенный сертификат центра сертификации
     * @throws CertificateProfileException если PEM некорректен или сертификат неподходящий
     */
    X509Certificate readCaPem(InputStream input) {
        try {
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(input);
            if (!(certificate instanceof X509Certificate)) {
                throw invalidCertificate(null);
            }
            X509Certificate caCertificate = (X509Certificate) certificate;
            caCertificate.checkValidity();
            return caCertificate;
        } catch (CertificateProfileException e) {
            throw e;
        } catch (Exception e) {
            throw invalidCertificate(e);
        }
    }

    /**
     * Проверяет наличие закрытого ключа, цепочки и действующего клиентского сертификата.
     *
     * @param entry найденная запись PKCS#12
     * @return проверенный материал клиентского ключа
     * @throws CertificateProfileException если запись неполна или сертификат просрочен
     */
    private ClientKeyMaterial validateClientEntry(KeyStore.PrivateKeyEntry entry) {
        PrivateKey privateKey = entry.getPrivateKey();
        Certificate[] chain = entry.getCertificateChain();
        if (privateKey == null || chain == null || chain.length == 0 || !(chain[0] instanceof X509Certificate)) {
            throw invalidCertificate(null);
        }
        try {
            ((X509Certificate) chain[0]).checkValidity();
        } catch (Exception e) {
            throw invalidCertificate(e);
        }
        return new ClientKeyMaterial(privateKey, chain);
    }

    /**
     * Создаёт ошибку открытия контейнера, не различая пароль, формат и повреждение без надёжных данных провайдера.
     *
     * @param cause исходная причина ошибки либо null
     * @return ошибка категории PKCS#12
     */
    private static CertificateProfileException invalidContainer(Throwable cause) {
        return new CertificateProfileException(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT,
                "PKCS#12 could not be opened", cause);
    }

    /**
     * Создаёт ошибку для контейнера, который открылся без доступного закрытого ключа.
     *
     * @param cause исходная причина ошибки либо null
     * @return ошибка извлечения закрытого ключа
     */
    private static CertificateProfileException unavailableKey(Throwable cause) {
        return new CertificateProfileException(CertificateProfileError.PKCS12_KEY_UNAVAILABLE,
                "PKCS#12 opened, but the private key is unavailable", cause);
    }

    /**
     * Создаёт ошибку непригодного сертификата.
     *
     * @param cause исходная причина ошибки либо null
     * @return ошибка категории сертификата
     */
    private static CertificateProfileException invalidCertificate(Throwable cause) {
        return new CertificateProfileException(CertificateProfileError.CERTIFICATE_INVALID,
                "Certificate material is unsuitable or expired", cause);
    }
}
