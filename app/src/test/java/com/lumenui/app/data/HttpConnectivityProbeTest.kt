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

    // --- Per-platform parsing ---------------------------------------------------------------
    //
    // These shapes are the ones the recorded probe bugs turned on: a wrong status is easy to
    // mistake for a plausible verdict on the device, so the parsing and grading are pinned here.

    @Test
    fun netflixPage_playsOnlyWhenTheRedirectStillNamesThatTitle() {
        val playing = probe.netflixPage("70143836", 301, "https://www.netflix.com/tw/title/70143836")
        assertTrue(playing.playable)
        assertEquals("TW", playing.region)

        // The language suffix must not leak into the region.
        val withLanguage = probe.netflixPage("80018499", 302, "https://www.netflix.com/jp-en/title/80018499")
        assertTrue(withLanguage.playable)
        assertEquals("JP", withLanguage.region)
    }

    @Test
    fun netflixPage_treatsARedirectAwayFromTheTitleAsUnavailable() {
        // What an address the catalogue will not serve gets: a redirect, but not to the title.
        assertFalse(probe.netflixPage("70143836", 302, "https://www.netflix.com/browse").playable)
        assertFalse(probe.netflixPage("70143836", 302, "https://www.netflix.com/unsupportedbrowser").playable)
        assertFalse(probe.netflixPage("70143836", 404, null).playable)
        assertNull(probe.netflixPage("70143836", 404, null).region)
    }

    @Test
    fun netflixStatus_gradesTheTwoTitlesIntoUnlockLimitedBlockedAndFailed() {
        val area = probe.netflixPage("80018499", 301, "https://www.netflix.com/us/title/80018499")
        val licensed = probe.netflixPage("70143836", 301, "https://www.netflix.com/us/title/70143836")
        val notServed = probe.netflixPage("70143836", 302, "https://www.netflix.com/browse")

        // Both titles play: the catalogue reaches past Netflix's own productions.
        assertEquals(ProbeStatus.UNLOCKED, probe.netflixStatus(area, licensed))
        // Only the self-test title plays: the region carries the service, originals only.
        assertEquals(ProbeStatus.LIMITED, probe.netflixStatus(area, notServed))
        // A 403 on either request is the address being refused outright.
        assertEquals(ProbeStatus.BLOCKED, probe.netflixStatus(probe.netflixPage("80018499", 403, null), licensed))
        assertEquals(ProbeStatus.BLOCKED, probe.netflixStatus(area, probe.netflixPage("70143836", 403, null)))
        // Neither request came back at all: a failed probe, never a region verdict.
        assertEquals(ProbeStatus.FAILED, probe.netflixStatus(null, null))
        // Both answered and neither played.
        assertEquals(
            ProbeStatus.BLOCKED,
            probe.netflixStatus(probe.netflixPage("80018499", 404, null), probe.netflixPage("70143836", 404, null))
        )
    }

    @Test
    fun hboRegion_scansEverySetCookieBecauseThereIsNoCountryCodeHeader() {
        // The real shape, and the reason reading header("countryCode") always missed it.
        assertEquals("TW", probe.hboRegion(listOf("countryCode=TW; path=/; domain=.hbomax.com")))
        // Several cookies, only one of them carries it, and it is not always uppercase.
        assertEquals("US", probe.hboRegion(listOf("session=x; path=/", "countryCode=us; path=/")))
        assertNull(probe.hboRegion(listOf("session=x; path=/")))
        assertNull(probe.hboRegion(emptyList()))
    }

    @Test
    fun hboOfferedRegions_alwaysCountsTheUsTheSiteOffers() {
        // The live page links each region as `"url":"/us/en"` — no trailing slash — which is what
        // HBO_COUNTRY_URL expects; the shape was checked against www.hbomax.com on 2026-10-16.
        assertEquals(setOf("US"), probe.hboOfferedRegions("""{"url":"/us/en"}"""))
        assertEquals(setOf("US", "TW"), probe.hboOfferedRegions("""{"url":"/us/en"},{"url":"/tw/zh"}"""))
        // A page that links no region at all still offers the US, or a US viewer reads as blocked.
        assertEquals(setOf("US"), probe.hboOfferedRegions(""))
    }

    @Test
    fun redditRegion_readsThePopOutOfServerTiming() {
        assertEquals("HK", probe.redditRegion("dn=FT,p=HKG,fw=0"))
        assertNull(probe.redditRegion("dn=FT"))
        assertNull(probe.redditRegion(null))
    }
}
