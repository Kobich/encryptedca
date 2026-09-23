package com.engboost.encryptedca.certificates;

/** State specific to the certificate import screen. */
final class CertificateImportState {
    enum Status { IDLE, IMPORTING, SUCCESS, ERROR }

    final Status status;
    final String profileId;
    final CertificateProfileError error;

    private CertificateImportState(Status status, String profileId, CertificateProfileError error) {
        this.status = status;
        this.profileId = profileId;
        this.error = error;
    }

    static CertificateImportState idle() { return new CertificateImportState(Status.IDLE, null, null); }
    static CertificateImportState importing() { return new CertificateImportState(Status.IMPORTING, null, null); }
    static CertificateImportState success(String id) { return new CertificateImportState(Status.SUCCESS, id, null); }
    static CertificateImportState error(CertificateProfileError error) {
        return new CertificateImportState(Status.ERROR, null, error);
    }
}
