package com.engboost.encryptedca.certificates;

import java.security.PrivateKey;
import java.security.cert.Certificate;

final class ClientKeyMaterial {
    final PrivateKey privateKey;
    final Certificate[] certificateChain;

    /**
     * Создаёт комплект клиентского ключа и его цепочки.
     *
     * @param privateKey закрытый клиентский ключ
     * @param certificateChain цепочка сертификатов клиента
     */
    ClientKeyMaterial(PrivateKey privateKey, Certificate[] certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }
}
