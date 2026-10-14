package com.lumenui.app.data

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the probe's pure verdict/marker helpers; the requests themselves need a network. */
class HttpConnectivityProbeTest {

    private val probe = HttpConnectivityProbe(OkHttpClient())

    @Test
    fun googleAbuseLocation_matchesTheCurrentHostAndTheRetiredOne() {
        assertTrue(
            probe.isGoogleAbuseLocation("https://www.google.com/sorry/index?continue=https://gemini.google.com/")
        )
        assertTrue(probe.isGoogleAbuseLocation("https://sorry.google.com/"))
        assertFalse(probe.isGoogleAbuseLocation("https://gemini.google.com/"))
        assertFalse(probe.isGoogleAbuseLocation(null))
    }

    @Test
    fun openAiStatus_readsOneObjectingEndpointAsAPartialVerdict() {
        assertEquals(ProbeStatus.BLOCKED, probe.openAiStatus(unsupportedCountry = true, vpnDetected = true))
        assertEquals(ProbeStatus.LIMITED, probe.openAiStatus(unsupportedCountry = true, vpnDetected = false))
        assertEquals(ProbeStatus.LIMITED, probe.openAiStatus(unsupportedCountry = false, vpnDetected = true))
        assertEquals(ProbeStatus.UNLOCKED, probe.openAiStatus(unsupportedCountry = false, vpnDetected = false))
    }

    @Test
    fun parseTrace_readsTheKeyValueBody() {
        val trace = probe.parseTrace(
            200,
            "fl=1a2b3c\nh=chatgpt.com\nip=165.84.140.225\nts=1.0\nloc=HK\ncolo=HKG\nwarp=off\n",
            null
        )
        assertEquals("165.84.140.225", trace.ip)
        assertEquals("HK", trace.loc)
        assertEquals("HKG", trace.colo)
        assertFalse(trace.warp)
    }

    @Test
    fun parseTrace_reportsWarpWhenItIsNotOff() {
        val trace = probe.parseTrace(200, "ip=1.2.3.4\nloc=US\ncolo=IAD\nwarp=on\n", null)
        assertTrue(trace.warp)
    }

    @Test
    fun parseTrace_fallsBackToTheCfRayColo() {
        val trace = probe.parseTrace(200, "ip=1.2.3.4\nloc=HK\n", "a4a7e6b7af579a34-HKG")
        assertEquals("HKG", trace.colo)
    }

    @Test
    fun parseTrace_yieldsNothingForAChallengePage() {
        val trace = probe.parseTrace(403, "<!DOCTYPE html><html><body>challenge</body></html>", null)
        assertNull(trace.ip)
        assertNull(trace.loc)
        assertNull(trace.colo)
        assertFalse(trace.warp)
    }
}
