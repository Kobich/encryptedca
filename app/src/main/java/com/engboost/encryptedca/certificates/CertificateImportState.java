package com.engboost.encryptedca.certificates;

/** Состояние экрана импорта сертификатного профиля. */
final class CertificateImportState {
    enum Status { IDLE, IMPORTING, SUCCESS, ERROR }

    final Status status;
    final String profileId;
    final CertificateProfileError error;

    /**
     * Создаёт состояние экрана импорта.
     *
     * @param status текущий этап операции
     * @param profileId идентификатор созданного профиля или null
     * @param error код ошибки или null
     */
    private CertificateImportState(Status status, String profileId, CertificateProfileError error) {
        this.status = status;
        this.profileId = profileId;
        this.error = error;
    }

    /**
     * Создаёт начальное состояние.
     *
     * @return состояние без выполняющейся операции
     */
    static CertificateImportState idle() { return new CertificateImportState(Status.IDLE, null, null); }

    /**
     * Создаёт состояние выполняющегося импорта.
     *
     * @return состояние ожидания результата
     */
    static CertificateImportState importing() { return new CertificateImportState(Status.IMPORTING, null, null); }

    /**
     * Создаёт состояние успешно завершённого импорта.
     *
     * @param id идентификатор сохранённого профиля
     * @return состояние успеха
     */
    static CertificateImportState success(String id) { return new CertificateImportState(Status.SUCCESS, id, null); }

    /**
     * Создаёт состояние завершения с ошибкой.
     *
     * @param error категория возникшей ошибки
     * @return состояние ошибки
     */
    static CertificateImportState error(CertificateProfileError error) {
        return new CertificateImportState(Status.ERROR, null, error);
    }
}
