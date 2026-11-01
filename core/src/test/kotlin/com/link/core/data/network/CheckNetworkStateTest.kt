package com.link.core.data.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckNetworkStateTest {

    private val network = mockk<Network>()

    private fun sut(active: Network?, caps: NetworkCapabilities?, vararg transports: Int): CheckNetworkStateImpl {
        val cm = mockk<ConnectivityManager> {
            every { activeNetwork } returns active
            every { getNetworkCapabilities(any()) } returns caps
        }
        caps?.let { c ->
            every { c.hasTransport(any()) } answers { firstArg<Int>() in transports }
        }
        return CheckNetworkStateImpl(cm)
    }

    @Test fun `no active network is offline`() = assertFalse(sut(null, null).isConnected())

    @Test fun `active network without capabilities is offline`() =
        assertFalse(sut(network, null).isConnected())

    @Test fun `wifi cellular ethernet are online`() {
        for (t in listOf(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_CELLULAR, NetworkCapabilities.TRANSPORT_ETHERNET)) {
            assertTrue("transport $t", sut(network, mockk(), t).isConnected())
        }
    }

    @Test fun `bluetooth only is offline`() =
        assertFalse(sut(network, mockk(), NetworkCapabilities.TRANSPORT_BLUETOOTH).isConnected())

    /** VPN-only report (no underlying transport flag) is treated as offline. */
    @Test fun `vpn only is offline by current definition`() =
        assertFalse(sut(network, mockk(), NetworkCapabilities.TRANSPORT_VPN).isConnected())

    @Test fun `no transports is offline`() = assertFalse(sut(network, mockk()).isConnected())
}
