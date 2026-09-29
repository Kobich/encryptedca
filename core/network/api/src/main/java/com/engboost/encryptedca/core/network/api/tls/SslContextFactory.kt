// Builds the SSLContext for mTLS from a profile's credentials.
// Throws TlsSetupException when the context can't be created.
package com.engboost.encryptedca.core.network.api.tls

import com.engboost.encryptedca.core.certificates.api.model.ClientCredentials
import javax.net.ssl.SSLContext

interface SslContextFactory {
    fun create(credentials: ClientCredentials): SSLContext
}

class TlsSetupException(message: String, cause: Throwable) : Exception(message, cause)
