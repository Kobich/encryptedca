package com.engboost.encryptedca.certificates;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

/** Restricts client-certificate selection to one previously imported alias. */
public final class SelectedAliasKeyManager extends X509ExtendedKeyManager {
    private final X509ExtendedKeyManager delegate;
    private final String selectedAlias;

    public SelectedAliasKeyManager(X509ExtendedKeyManager delegate, String selectedAlias) {
        if (delegate == null || selectedAlias == null) throw new IllegalArgumentException("Delegate and alias are required");
        this.delegate = delegate;
        this.selectedAlias = selectedAlias;
    }

    @Override public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return isUsable(keyTypes, issuers) ? selectedAlias : null;
    }

    @Override public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return isUsable(keyTypes, issuers) ? selectedAlias : null;
    }

    @Override public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate.getClientAliases(keyType, issuers);
    }

    @Override public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate.getServerAliases(keyType, issuers);
    }

    @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return delegate.chooseServerAlias(keyType, issuers, socket);
    }

    @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return delegate.chooseEngineServerAlias(keyType, issuers, engine);
    }

    @Override public X509Certificate[] getCertificateChain(String alias) { return delegate.getCertificateChain(alias); }
    @Override public PrivateKey getPrivateKey(String alias) { return delegate.getPrivateKey(alias); }

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
