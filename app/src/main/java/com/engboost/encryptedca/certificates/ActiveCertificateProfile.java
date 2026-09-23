package com.engboost.encryptedca.certificates;

import java.security.cert.X509Certificate;

/** Immutable description of a profile that is ready for TLS use. */
public final class ActiveCertificateProfile {
    private final String profileId;
    private final String displayName;
    private final String clientKeyAlias;
    private final X509Certificate caCertificate;

    public ActiveCertificateProfile(String profileId, String displayName,
                                    String clientKeyAlias, X509Certificate caCertificate) {
        this.profileId = profileId;
        this.displayName = displayName;
        this.clientKeyAlias = clientKeyAlias;
        this.caCertificate = caCertificate;
    }

    public String getProfileId() { return profileId; }
    public String getDisplayName() { return displayName; }
    public String getClientKeyAlias() { return clientKeyAlias; }
    public X509Certificate getCaCertificate() { return caCertificate; }
}
