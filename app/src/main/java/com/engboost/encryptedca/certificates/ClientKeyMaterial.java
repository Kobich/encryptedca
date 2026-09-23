package com.engboost.encryptedca.certificates;

import java.security.PrivateKey;
import java.security.cert.Certificate;

final class ClientKeyMaterial {
    final PrivateKey privateKey;
    final Certificate[] certificateChain;

    ClientKeyMaterial(PrivateKey privateKey, Certificate[] certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }
}
