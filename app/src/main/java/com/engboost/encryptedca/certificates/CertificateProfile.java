package com.engboost.encryptedca.certificates;

import java.security.cert.X509Certificate;

/** Неизменяемое описание профиля, прочитанное из хранилища. */
public final class CertificateProfile {
    private final String profileId;
    private final String displayName;
    private final String clientKeyAlias;
    private final X509Certificate caCertificate;

    /**
     * Создаёт описание сохранённого профиля.
     *
     * @param profileId идентификатор профиля
     * @param displayName отображаемое имя или null
     * @param clientKeyAlias alias клиентского ключа
     * @param caCertificate сертификат центра сертификации
     */
    public CertificateProfile(String profileId, String displayName,
                              String clientKeyAlias, X509Certificate caCertificate) {
        this.profileId = profileId;
        this.displayName = displayName;
        this.clientKeyAlias = clientKeyAlias;
        this.caCertificate = caCertificate;
    }

    /** Возвращает идентификатор профиля.
     * @return идентификатор профиля
     */
    public String getProfileId() { return profileId; }
    /** Возвращает имя профиля.
     * @return имя либо null
     */
    public String getDisplayName() { return displayName; }
    /** Возвращает alias клиентского ключа.
     * @return alias Android Keystore
     */
    public String getClientKeyAlias() { return clientKeyAlias; }
    /** Возвращает сертификат центра сертификации.
     * @return сертификат CA
     */
    public X509Certificate getCaCertificate() { return caCertificate; }
}
