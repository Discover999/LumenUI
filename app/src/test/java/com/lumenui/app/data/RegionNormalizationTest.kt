package com.lumenui.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionNormalizationTest {

    @Test
    fun twoLetterCodesPassThroughUppercased() {
        assertEquals("US", HttpConnectivityProbe.normalizeRegion("US"))
        assertEquals("US", HttpConnectivityProbe.normalizeRegion("us"))
        assertEquals("JP", HttpConnectivityProbe.normalizeRegion("jp"))
    }

    @Test
    fun threeLetterIsoCodesAreMapped() {
        assertEquals("TW", HttpConnectivityProbe.normalizeRegion("TWN"))
        assertEquals("JP", HttpConnectivityProbe.normalizeRegion("JPN"))
        assertEquals("GB", HttpConnectivityProbe.normalizeRegion("GBR"))
        assertEquals("DE", HttpConnectivityProbe.normalizeRegion("DEU"))
        assertEquals("CN", HttpConnectivityProbe.normalizeRegion("CHN"))
    }

    @Test
    fun awsStyleEdgeRegionsAreMapped() {
        assertEquals("US", HttpConnectivityProbe.normalizeRegion("EASTUS2"))
        assertEquals("NL", HttpConnectivityProbe.normalizeRegion("WESTEUROPE"))
        assertEquals("SG", HttpConnectivityProbe.normalizeRegion("SOUTHEASTASIA"))
        assertEquals("JP", HttpConnectivityProbe.normalizeRegion("JAPANEAST"))
    }

    @Test
    fun unknownCodeReturnsNull() {
        assertNull(HttpConnectivityProbe.normalizeRegion("XYZ"))
    }
}
