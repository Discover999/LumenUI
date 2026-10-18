package com.lumenui.app.ui

import com.lumenui.app.data.SpeedSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveRateTest {

    @Test
    fun accept_waitsForTheFirstWholeSecondThenAveragesTheTrailingSecond() {
        val live = LiveRate()

        assertNull(live.accept(SpeedSample(250, 10.0)))
        assertNull(live.accept(SpeedSample(500, 20.0)))
        assertNull(live.accept(SpeedSample(750, 30.0)))
        assertEquals(25.0, live.accept(SpeedSample(1000, 40.0))!!, 0.001)

        assertNull(live.accept(SpeedSample(1250, 50.0)))
        assertNull(live.accept(SpeedSample(1500, 60.0)))
        assertNull(live.accept(SpeedSample(1750, 70.0)))
        assertEquals(65.0, live.accept(SpeedSample(2000, 80.0))!!, 0.001)
    }

    @Test
    fun accept_updatesOnTheNextWholeSecondEvenIfTheExactBoundaryWasMissed() {
        val live = LiveRate()

        assertEquals(10.0, live.accept(SpeedSample(1200, 10.0))!!, 0.001)
        assertNull(live.accept(SpeedSample(1300, 10.0)))
        assertEquals(10.0, live.accept(SpeedSample(2100, 10.0))!!, 0.001)
    }

    @Test
    fun accept_dropsThePreviousSecondsSamples() {
        val live = LiveRate()

        assertNull(live.accept(SpeedSample(250, 10.0)))
        assertEquals(25.0, live.accept(SpeedSample(1000, 40.0))!!, 0.001)
        assertEquals(60.0, live.accept(SpeedSample(2000, 60.0))!!, 0.001)
    }
}
