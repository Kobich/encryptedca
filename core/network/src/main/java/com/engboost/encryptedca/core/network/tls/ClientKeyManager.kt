package com.engboost.encryptedca.core.network.tls

import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/** Presents the profile's key during the handshake, and only as a client: the phone never acts as a TLS server. */
internal class ClientKeyManager(
    private val privateKey: PrivateKey,
    private val certificateChain: Array<X509Certificate>,
) : X509ExtendedKeyManager() {

    override fun chooseClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) =
        ALIAS.takeIf { isUsable(keyTypes, issuers) }

    override fun chooseEngineClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?) =
        ALIAS.takeIf { isUsable(keyTypes, issuers) }

    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) =
        arrayOf(ALIAS).takeIf { isUsable(keyType?.let { arrayOf(it) }, issuers) }

    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null

    override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String? = null

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? = certificateChain.takeIf { alias == ALIAS }

    override fun getPrivateKey(alias: String?): PrivateKey? = privateKey.takeIf { alias == ALIAS }

    private fun isUsable(keyTypes: Array<out String>?, issuers: Array<out Principal>?): Boolean {
        val typeMatches = keyTypes.isNullOrEmpty() || keyTypes.any { matchesKeyType(it, privateKey.algorithm) }
        val issuerMatches = issuers.isNullOrEmpty() || issuers.any { requested ->
            certificateChain.any { it.issuerX500Principal == requested || it.subjectX500Principal == requested }
        }
        return typeMatches && issuerMatches
    }

    // Besides plain "RSA"/"EC", JSSE uses "<key>_<signer>" names such as "EC_RSA".
    private fun matchesKeyType(keyType: String, algorithm: String) =
        keyType.equals(algorithm, ignoreCase = true) || keyType.startsWith("${algorithm}_", ignoreCase = true)

    private companion object {
        const val ALIAS = "profile"
    }
}
