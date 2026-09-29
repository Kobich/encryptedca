// Результат проверки хоста:
// TRUSTED — mTLS-рукопожатие прошло, сертификат сервера ведёт к CA профиля;
// HANDSHAKE_FAILED — порт открыт, но рукопожатие не прошло;
// PORT_UNREACHABLE — хост жив, но к порту подключиться не удалось.
// serverFingerprint — SHA-256 сертификата, который показало доверенное устройство.
package com.engboost.encryptedca.core.network.scan

import java.net.Inet4Address

enum class DeviceStatus {
    TRUSTED,
    HANDSHAKE_FAILED,
    PORT_UNREACHABLE,
}

data class FoundDevice(
    val address: Inet4Address,
    val status: DeviceStatus,
    val serverFingerprint: String? = null,
) : Comparable<FoundDevice> {
    val ip: String get() = address.hostAddress.orEmpty()

    override fun compareTo(other: FoundDevice) = address.toUInt().compareTo(other.address.toUInt())
}
