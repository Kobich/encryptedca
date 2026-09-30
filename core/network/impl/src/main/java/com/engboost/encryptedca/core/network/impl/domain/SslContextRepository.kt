package com.engboost.encryptedca.core.network.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import javax.net.ssl.SSLContext

internal interface SslContextRepository {
    fun create(credentials: ClientCredentials): SSLContext
}
