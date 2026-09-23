package com.engboost.encryptedca.certificates;

/** Ошибка, возникающая при небезопасном импорте или чтении сертификатного профиля. */
public final class CertificateProfileException extends RuntimeException {
    private final CertificateProfileError error;

    /**
     * Создаёт ошибку профиля без исходной причины.
     *
     * @param error категория ошибки
     * @param message техническое описание для диагностики
     */
    public CertificateProfileException(CertificateProfileError error, String message) {
        super(message);
        this.error = error;
    }

    /**
     * Создаёт ошибку профиля и сохраняет исходную причину.
     *
     * @param error категория ошибки
     * @param message техническое описание для диагностики
     * @param cause исходное исключение
     */
    public CertificateProfileException(CertificateProfileError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    /**
     * Возвращает категорию ошибки для выбора пользовательского сообщения.
     *
     * @return категория ошибки
     */
    public CertificateProfileError getError() { return error; }
}
