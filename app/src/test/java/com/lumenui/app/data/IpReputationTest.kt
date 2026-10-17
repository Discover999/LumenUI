package com.lumenui.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

private val TestJson = Json { ignoreUnknownKeys = true }

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

    @Test
    fun risk_proxyOrHostingIsHigh() {
        assertEquals(IpRisk.HIGH, IpReputation(proxy = true).risk)
        assertEquals(IpRisk.HIGH, IpReputation(hosting = true).risk)
        assertEquals(IpRisk.HIGH, IpReputation(hosting = true, reverse = "one.one.one.one").risk)
    }

    @Test
    fun risk_missingReverseIsMedium() {
        assertEquals(IpRisk.MEDIUM, IpReputation(reverse = "").risk)
    }

    @Test
    fun risk_reversePresentAndNoFlagsIsLow() {
        assertEquals(IpRisk.LOW, IpReputation(reverse = "host-1.example.net").risk)
        assertEquals(IpRisk.LOW, IpReputation(mobile = true, reverse = "host-1.example.net").risk)
    }

    @Test
    fun riskSignals_nameTheObservationsBehindTheGrade() {
        assertEquals(listOf("无代理/机房标记"), IpReputation(reverse = "ptr.example").riskSignals)
        assertEquals(
            listOf("代理标记", "无反向解析"),
            IpReputation(proxy = true).riskSignals
        )
        assertEquals(
            listOf("机房 IP", "无反向解析"),
            IpReputation(hosting = true).riskSignals
        )
    }

    @Test
    fun operator_isIspPlusAsNumberWithoutRepeatingTheName() {
        val rep = IpReputation(
            isp = "Cloudflare, Inc",
            asn = "AS13335 Cloudflare, Inc.",
            asname = "CLOUDFLARENET"
        )
        assertEquals("Cloudflare, Inc · AS13335", rep.operator)
    }

    @Test
    fun operator_fallsBackToOrgThenAsNameAndSkipsAWordlessAsn() {
        assertEquals("Example Org · AS64500", IpReputation(org = "Example Org", asn = "AS64500 Example").operator)
        assertEquals("EXAMPLE-AS", IpReputation(asname = "EXAMPLE-AS", asn = "Example").operator)
        assertEquals("", IpReputation().operator)
    }

    @Test
    fun location_prefersCountryCodeAndSkipsBlankParts() {
        assertEquals(
            "South Brisbane · AU",
            IpReputation(city = "South Brisbane", country = "Australia", countryCode = "AU").location
        )
        assertEquals("AU", IpReputation(country = "Australia", countryCode = "AU").location)
        assertEquals("Australia", IpReputation(country = "Australia").location)
        assertEquals("", IpReputation().location)
    }

    @Test
    fun json_mapsTheAsFieldAndTheProfileDetails() {
        val payload = """
            {"status":"success","country":"Australia","countryCode":"AU",
             "city":"South Brisbane","isp":"Cloudflare, Inc",
             "org":"APNIC and Cloudflare DNS Resolver project",
             "as":"AS13335 Cloudflare, Inc.","asname":"CLOUDFLARENET",
             "reverse":"one.one.one.one","mobile":false,"proxy":false,"hosting":true}
        """.trimIndent()
        val rep = TestJson.decodeFromString<IpReputation>(payload)
        assertEquals("AS13335", rep.asNumber)
        assertEquals("Cloudflare, Inc · AS13335", rep.operator)
        assertEquals("South Brisbane · AU", rep.location)
        assertEquals("one.one.one.one", rep.reverse)
        assertEquals(IpRisk.HIGH, rep.risk)
    }
}
