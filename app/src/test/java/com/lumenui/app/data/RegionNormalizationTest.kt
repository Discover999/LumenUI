package com.lumenui.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionNormalizationTest {

    @Test
    fun twoLetterCodesPassThroughUppercased() {
        assertEquals("US", ConnectivityProbe.normalizeRegion("US"))
        assertEquals("US", ConnectivityProbe.normalizeRegion("us"))
        assertEquals("JP", ConnectivityProbe.normalizeRegion("jp"))
    }

    @Test
    fun threeLetterIsoCodesAreMapped() {
        assertEquals("TW", ConnectivityProbe.normalizeRegion("TWN"))
        assertEquals("JP", ConnectivityProbe.normalizeRegion("JPN"))
        assertEquals("GB", ConnectivityProbe.normalizeRegion("GBR"))
        assertEquals("DE", ConnectivityProbe.normalizeRegion("DEU"))
        assertEquals("CN", ConnectivityProbe.normalizeRegion("CHN"))
    }

    @Test
    fun awsStyleEdgeRegionsAreMapped() {
        assertEquals("US", ConnectivityProbe.normalizeRegion("EASTUS2"))
        assertEquals("NL", ConnectivityProbe.normalizeRegion("WESTEUROPE"))
        assertEquals("SG", ConnectivityProbe.normalizeRegion("SOUTHEASTASIA"))
        assertEquals("JP", ConnectivityProbe.normalizeRegion("JAPANEAST"))
    }

    @Test
    fun unknownCodeReturnsNull() {
        assertNull(ConnectivityProbe.normalizeRegion("XYZ"))
    }
}
