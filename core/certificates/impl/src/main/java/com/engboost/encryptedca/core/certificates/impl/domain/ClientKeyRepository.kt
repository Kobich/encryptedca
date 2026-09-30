package com.engboost.encryptedca.core.certificates.impl.domain

import com.engboost.encryptedca.core.certificates.impl.domain.parsing.PrivateKeyWithChain
import java.security.KeyStore

internal interface ClientKeyRepository {
    fun save(profileId: String, key: PrivateKeyWithChain)

    fun read(profileId: String): KeyStore.PrivateKeyEntry

    fun exists(profileId: String): Boolean

    fun delete(profileId: String)
}
