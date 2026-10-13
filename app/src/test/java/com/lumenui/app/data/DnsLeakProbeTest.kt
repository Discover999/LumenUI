package com.lumenui.app.data

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsLeakProbeTest {

    private val probe = DnsLeakProbe(OkHttpClient())

    private val sampleHtml = """
        <div class="conclusion">DNS may be leaking.</div>
        <div class="tr">
          <div class="td" data-host="1.1.1.1" data-ctr="us" data-ctrname="United States"></div>
          <div class="td">AS13335</div>
        </div>
        <div class="tr">
          <div class="td" data-host="8.8.8.8" data-ctr="JP" data-ctrname="Japan"></div>
          <div class="td">AS15169</div>
        </div>
    """.trimIndent()

    @Test
    fun parse_readsResolversAndConclusion() {
        val result = probe.parse(sampleHtml)
        assertEquals(2, result.resolvers.size)

        val first = result.resolvers[0]
        assertEquals("1.1.1.1", first.ip)
        assertEquals("US", first.countryCode)
        assertEquals("United States", first.countryName)
        assertEquals("AS13335", first.asn)

        assertEquals("8.8.8.8", result.resolvers[1].ip)
        assertEquals("DNS may be leaking.", result.conclusion)
    }

    @Test
    fun parse_handlesEmptyHtml() {
        val result = probe.parse("")
        assertTrue(result.resolvers.isEmpty())
        assertNull(result.conclusion)
    }

    @Test
    fun leaking_trueWhenConclusionStatesLeak() {
        assertEquals(true, DnsLeakResult(emptyList(), "DNS may be leaking").leaking)
    }

    @Test
    fun leaking_falseWhenConclusionDeniesLeak() {
        assertEquals(false, DnsLeakResult(emptyList(), "DNS is not leaking").leaking)
        assertEquals(false, DnsLeakResult(emptyList(), "no leak").leaking)
    }

    @Test
    fun leaking_nullWhenUndecided() {
        assertNull(DnsLeakResult(emptyList(), null).leaking)
        assertNull(DnsLeakResult(emptyList(), "something else").leaking)
    }
}
