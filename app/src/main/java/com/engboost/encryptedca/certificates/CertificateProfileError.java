package com.engboost.encryptedca.certificates;

/** Коды ошибок, по которым экран выбирает пользовательское сообщение. */
public enum CertificateProfileError {
    /** Не удалось получить доступ к выбранному документу. */
    FILE_UNAVAILABLE,
    /** Контейнер повреждён либо пароль не принят PKCS#12-провайдером. */
    PKCS12_PASSWORD_OR_CORRUPT,
    /** Сертификат просрочен, ещё не действует или не подходит по назначению. */
    CERTIFICATE_INVALID,
    /** Не удалось сохранить или прочитать материал профиля. */
    STORAGE_FAILED,
    /** В индексе есть профиль, но его ключ или CA отсутствует. */
    PROFILE_INCOMPLETE
}
