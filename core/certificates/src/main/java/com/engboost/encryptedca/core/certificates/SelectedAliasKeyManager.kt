package com.engboost.encryptedca.core.certificates

import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/**
 * The AndroidKeyStore key manager offers every key it holds. This one presents only the profile's
 * key, and only as a client: the device never acts as a TLS server.
 */
internal class SelectedAliasKeyManager(
    private val delegate: X509ExtendedKeyManager,
    private val alias: String,
) : X509ExtendedKeyManager() {

    override fun chooseClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) =
        alias.takeIf { isUsable(keyTypes, issuers) }

    override fun chooseEngineClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?) =
        alias.takeIf { isUsable(keyTypes, issuers) }

    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) =
        arrayOf(alias).takeIf { isUsable(keyType?.let { arrayOf(it) }, issuers) }

    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null

    override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String? = null

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
        if (alias == this.alias) delegate.getCertificateChain(alias) else null

    override fun getPrivateKey(alias: String?): PrivateKey? =
        if (alias == this.alias) delegate.getPrivateKey(alias) else null

    private fun isUsable(keyTypes: Array<out String>?, issuers: Array<out Principal>?): Boolean {
        val key = delegate.getPrivateKey(alias) ?: return false
        val chain = delegate.getCertificateChain(alias)?.takeIf { it.isNotEmpty() } ?: return false
        val typeMatches = keyTypes.isNullOrEmpty() || keyTypes.any { matchesKeyType(it, key.algorithm) }
        val issuerMatches = issuers.isNullOrEmpty() || issuers.any { requested ->
            chain.any { it.issuerX500Principal == requested || it.subjectX500Principal == requested }
        }
        return typeMatches && issuerMatches
    }

    // Besides plain "RSA"/"EC", JSSE uses "<key>_<signer>" names such as "EC_RSA".
    private fun matchesKeyType(keyType: String, algorithm: String) =
        keyType.equals(algorithm, ignoreCase = true) || keyType.startsWith("${algorithm}_", ignoreCase = true)
}
