package com.engboost.encryptedca.certificates;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/** Читает и проверяет сертификаты; входные потоки остаются у вызывающего кода. */
final class CertificateMaterialReader {
    /**
     * Читает PKCS#12, находит закрытый ключ и проверяет клиентскую цепочку.
     *
     * @param input поток PKCS#12; остаётся во владении вызывающего кода
     * @param password пароль контейнера
     * @return проверенный закрытый ключ и цепочка сертификатов
     * @throws CertificateProfileException если контейнер повреждён, пароль не принят или сертификат непригоден
     */
    ClientKeyMaterial readPkcs12(InputStream input, char[] password) {
        try {
            KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(input, password);
            for (Enumeration<String> aliases = pkcs12.aliases(); aliases.hasMoreElements();) {
                String alias = aliases.nextElement();
                if (!pkcs12.isKeyEntry(alias)) {
                    continue;
                }
                KeyStore.Entry entry = pkcs12.getEntry(alias, new KeyStore.PasswordProtection(password));
                if (entry instanceof KeyStore.PrivateKeyEntry) {
                    return validateClientEntry((KeyStore.PrivateKeyEntry) entry);
                }
            }
            throw invalidContainer(null);
        } catch (CertificateProfileException e) {
            throw e;
        } catch (IOException e) {
            // Провайдеры PKCS#12 сообщают IOException и при неверном пароле, и при повреждении.
            throw invalidContainer(e);
        } catch (Exception e) {
            throw invalidContainer(e);
        }
    }

    /**
     * Разбирает CA PEM и проверяет тип, срок действия и признак CA.
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
            if (caCertificate.getBasicConstraints() < 0) {
                throw invalidCertificate(null);
            }
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
     * Создаёт ошибку чтения контейнера, не различая пароль и повреждение без надёжных данных провайдера.
     *
     * @param cause исходная причина ошибки либо null
     * @return ошибка категории PKCS#12
     */
    private static CertificateProfileException invalidContainer(Throwable cause) {
        return new CertificateProfileException(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT,
                "PKCS#12 password is invalid or container is damaged", cause);
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
