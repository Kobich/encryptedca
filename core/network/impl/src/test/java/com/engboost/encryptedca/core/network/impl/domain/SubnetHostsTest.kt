package com.engboost.encryptedca.core.network.impl.domain

import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class SubnetHostsTest {

    @Test
    fun slash24SkipsNetworkBroadcastAndOwnAddress() {
        val hosts = hostsToScan("192.168.1.10", 24)

        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertFalse("192.168.1.10" in hosts)
    }

    @Test
    fun widerSubnetIsCutDownToOwnSlash24() {
        val hosts = hostsToScan("10.0.5.7", 16)

        assertEquals(253, hosts.size)
        assertTrue(hosts.all { it.startsWith("10.0.5.") })
    }

    @Test
    fun slash30LeavesTheOtherHost() {
        assertEquals(listOf("192.168.1.2"), hostsToScan("192.168.1.1", 30))
    }

    @Test
    fun slash32HasNothingToScan() {
        assertTrue(hostsToScan("192.168.1.1", 32).isEmpty())
    }

    private fun hostsToScan(address: String, prefixLength: Int): List<String> =
        LocalNetwork(networkHandle = 0, address = InetAddress.getByName(address) as Inet4Address, prefixLength = prefixLength)
            .hostsToScan()
            .map { it.hostAddress.orEmpty() }
}
