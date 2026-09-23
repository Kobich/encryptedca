package com.engboost.encryptedca.certificates;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/** Reads and validates certificate material; callers retain ownership of input streams. */
final class CertificateMaterialReader {
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
            // PKCS#12 providers use IOException for both integrity/password failures and corruption.
            throw invalidContainer(e);
        } catch (Exception e) {
            throw invalidContainer(e);
        }
    }

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

    private static CertificateProfileException invalidContainer(Throwable cause) {
        return new CertificateProfileException(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT,
                "PKCS#12 password is invalid or container is damaged", cause);
    }

    private static CertificateProfileException invalidCertificate(Throwable cause) {
        return new CertificateProfileException(CertificateProfileError.CERTIFICATE_INVALID,
                "Certificate material is unsuitable or expired", cause);
    }
}
