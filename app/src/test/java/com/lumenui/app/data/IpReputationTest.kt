package com.lumenui.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IpReputationTest {

    @Test
    fun label_residentialWhenNoFlags() {
        assertEquals("住宅", IpReputation().label)
    }

    @Test
    fun label_mapsEachFlag() {
        assertEquals("代理", IpReputation(proxy = true).label)
        assertEquals("机房", IpReputation(hosting = true).label)
        assertEquals("移动", IpReputation(mobile = true).label)
    }

    @Test
    fun label_proxyTakesPrecedence() {
        assertEquals("代理", IpReputation(proxy = true, hosting = true, mobile = true).label)
        assertEquals("机房", IpReputation(hosting = true, mobile = true).label)
    }
}
