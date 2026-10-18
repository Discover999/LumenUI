package com.lumenui.app.ui.components

import com.lumenui.app.data.SpeedSample
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedChartSmoothingTest {

    @Test
    fun smooth_averagesTheTrailingSecond() {
        val samples = listOf(
            SpeedSample(250, 10.0),
            SpeedSample(500, 20.0),
            SpeedSample(750, 30.0),
            SpeedSample(1000, 40.0),
            SpeedSample(1250, 50.0)
        )

        val smoothed = smooth(samples)

        assertEquals(10.0, smoothed[0].mbps, 0.001)
        assertEquals(15.0, smoothed[1].mbps, 0.001)
        assertEquals(20.0, smoothed[2].mbps, 0.001)
        assertEquals(25.0, smoothed[3].mbps, 0.001)
        assertEquals(35.0, smoothed[4].mbps, 0.001)
    }

    @Test
    fun smooth_keepsEachSamplesOwnTime() {
        val samples = listOf(SpeedSample(250, 10.0), SpeedSample(1000, 40.0))

        assertEquals(listOf(250L, 1000L), smooth(samples).map { it.elapsedMs })
    }

    @Test
    fun smooth_leavesASingleSampleAlone() {
        val samples = listOf(SpeedSample(250, 42.0))

        assertEquals(samples, smooth(samples))
    }
}
