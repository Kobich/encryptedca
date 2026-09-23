package com.engboost.encryptedca.certificates;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

/** Ограничивает выбор клиентского сертификата ранее импортированным alias профиля. */
public final class SelectedAliasKeyManager extends X509ExtendedKeyManager {
    private final X509ExtendedKeyManager delegate;
    private final String selectedAlias;

    /**
     * Создаёт менеджер, ограничивающий выбор клиентского ключа заданным alias.
     *
     * @param delegate исходный менеджер ключей
     * @param selectedAlias alias профиля, который разрешено выбрать
     * @throws IllegalArgumentException если менеджер или alias не заданы
     */
    public SelectedAliasKeyManager(X509ExtendedKeyManager delegate, String selectedAlias) {
        if (delegate == null || selectedAlias == null) throw new IllegalArgumentException("Delegate and alias are required");
        this.delegate = delegate;
        this.selectedAlias = selectedAlias;
    }

    /**
     * Возвращает выбранный alias, если ключ подходит по типу и issuer.
     *
     * @param keyTypes запрошенные типы ключей
     * @param issuers допустимые центры выдачи
     * @param socket TLS-сокет или null
     * @return выбранный alias либо null
     */
    @Override public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return isUsable(keyTypes, issuers) ? selectedAlias : null;
    }

    /**
     * Выбирает alias для TLS-соединения через SSLEngine.
     *
     * @param keyTypes запрошенные типы ключей
     * @param issuers допустимые центры выдачи
     * @param engine TLS-движок или null
     * @return выбранный alias либо null
     */
    @Override public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return isUsable(keyTypes, issuers) ? selectedAlias : null;
    }

    /**
     * Делегирует получение клиентских alias исходному менеджеру.
     *
     * @param keyType тип ключа
     * @param issuers допустимые центры выдачи
     * @return подходящие alias либо null
     */
    @Override public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate.getClientAliases(keyType, issuers);
    }

    /**
     * Делегирует получение серверных alias исходному менеджеру.
     *
     * @param keyType тип ключа
     * @param issuers допустимые центры выдачи
     * @return подходящие alias либо null
     */
    @Override public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate.getServerAliases(keyType, issuers);
    }

    /**
     * Делегирует выбор серверного alias исходному менеджеру.
     *
     * @param keyType тип ключа
     * @param issuers допустимые центры выдачи
     * @param socket TLS-сокет
     * @return выбранный серверный alias либо null
     */
    @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return delegate.chooseServerAlias(keyType, issuers, socket);
    }

    /**
     * Делегирует выбор серверного alias для SSLEngine.
     *
     * @param keyType тип ключа
     * @param issuers допустимые центры выдачи
     * @param engine TLS-движок
     * @return выбранный серверный alias либо null
     */
    @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return delegate.chooseEngineServerAlias(keyType, issuers, engine);
    }

    /**
     * Возвращает цепочку сертификатов через исходный менеджер.
     *
     * @param alias alias ключа
     * @return цепочка сертификатов либо null
     */
    @Override public X509Certificate[] getCertificateChain(String alias) { return delegate.getCertificateChain(alias); }

    /**
     * Возвращает закрытый ключ через исходный менеджер.
     *
     * @param alias alias ключа
     * @return закрытый ключ либо null
     */
    @Override public PrivateKey getPrivateKey(String alias) { return delegate.getPrivateKey(alias); }

    /**
     * Проверяет наличие ключа, соответствие keyType и issuer выбранного профиля.
     *
     * @param keyTypes запрошенные типы ключей
     * @param issuers допустимые центры выдачи
     * @return true, если выбранный alias удовлетворяет запросу TLS
     */
    private boolean isUsable(String[] keyTypes, Principal[] issuers) {
        PrivateKey privateKey = delegate.getPrivateKey(selectedAlias);
        X509Certificate[] chain = delegate.getCertificateChain(selectedAlias);
        if (privateKey == null || chain == null || chain.length == 0) return false;
        if (keyTypes != null && keyTypes.length > 0) {
            boolean keyTypeMatches = false;
            for (String keyType : keyTypes) {
                if (keyType != null && privateKey.getAlgorithm().equalsIgnoreCase(keyType)) {
                    keyTypeMatches = true;
                    break;
                }
            }
            if (!keyTypeMatches) return false;
        }
        if (issuers == null || issuers.length == 0) return true;
        for (Principal requestedIssuer : issuers) {
            for (X509Certificate certificate : chain) {
                if (requestedIssuer.equals(certificate.getIssuerX500Principal())) return true;
            }
        }
        return false;
    }
}
