package com.lumenui.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedTestTest {

    @Test
    fun speedMbps_isRealBitsOverRealTime() {
        assertEquals(80.0, speedMbps(10_000_000, 1000), 0.001)
        assertEquals(0.0, speedMbps(1_000_000, 0), 0.001)
    }

    @Test
    fun jitter_isTheMeanAbsoluteDelta() {
        assertEquals(7.5, jitterMs(listOf(50L, 60L, 55L))!!, 0.001)
        assertNull(jitterMs(listOf(50L)))
        assertNull(jitterMs(emptyList()))
    }

    @Test
    fun grades_matchTheReferenceBands() {
        assertEquals(SpeedGrade.EXCELLENT, speedGrade(64.1))
        assertEquals(SpeedGrade.FAIR, speedGrade(0.9))
        assertEquals(SpeedGrade.GOOD, speedGrade(10.0))
        assertEquals(SpeedGrade.POOR, speedGrade(0.4))
        assertEquals(SpeedGrade.GOOD, latencyGrade(55L))
        assertEquals(SpeedGrade.EXCELLENT, latencyGrade(20L))
        assertEquals(SpeedGrade.FAIR, latencyGrade(100L))
        assertEquals(SpeedGrade.POOR, latencyGrade(200L))
        assertEquals(SpeedGrade.FAIR, jitterGrade(61.9))
        assertEquals(SpeedGrade.EXCELLENT, jitterGrade(5.0))
    }

    @Test
    fun formatMBps_dividesByEight() {
        assertEquals("10.0", formatMBps(80.0))
        assertEquals("0.1", formatMBps(0.9))
        assertEquals("—", formatMBps(null))
    }

    @Test
    fun formatBytes_usesDecimalUnits() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1.5 KB", formatBytes(1_500))
        assertEquals("12.3 MB", formatBytes(12_300_000))
        assertEquals("1.23 GB", formatBytes(1_234_000_000))
    }

    @Test
    fun transferTime_usesDecimalMegabytes() {
        assertEquals("2.5 小时", transferTime(1000, 0.9))
        assertEquals("12.5 秒钟", transferTime(100, 64.1))
        assertNull(transferTime(100, 0.0))
        assertNull(transferTime(100, null))
    }
}
