package com.lumenui.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionNormalizationTest {

    @Test
    fun twoLetterCodesPassThroughUppercased() {
        assertEquals("US", RegionCodes.normalize("US"))
        assertEquals("US", RegionCodes.normalize("us"))
        assertEquals("JP", RegionCodes.normalize("jp"))
    }

    @Test
    fun threeLetterIsoCodesAreMapped() {
        assertEquals("TW", RegionCodes.normalize("TWN"))
        assertEquals("JP", RegionCodes.normalize("JPN"))
        assertEquals("GB", RegionCodes.normalize("GBR"))
        assertEquals("DE", RegionCodes.normalize("DEU"))
        assertEquals("CN", RegionCodes.normalize("CHN"))
    }

    @Test
    fun awsStyleEdgeRegionsAreMapped() {
        assertEquals("US", RegionCodes.normalize("EASTUS2"))
        assertEquals("NL", RegionCodes.normalize("WESTEUROPE"))
        assertEquals("SG", RegionCodes.normalize("SOUTHEASTASIA"))
        assertEquals("JP", RegionCodes.normalize("JAPANEAST"))
    }

    @Test
    fun unknownCodeReturnsNull() {
        assertNull(RegionCodes.normalize("XYZ"))
    }
}
