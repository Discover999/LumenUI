package com.lumenui.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the pure formatting used by a node's latency readout. */
class DelayFormatTest {

    @Test
    fun delayColor_separatesUnknownFromFailed() {
        // Never tested and tested-but-no-answer are both muted: neither is a slow node.
        assertEquals(MutedColor, delayColor(null))
        assertEquals(MutedColor, delayColor(0))
        assertEquals(OkColor, delayColor(120))
        assertEquals(WarnColor, delayColor(500))
        assertEquals(BadColor, delayColor(900))
    }

    @Test
    fun delayLabel_namesTheTimeoutRatherThanPrintingZero() {
        assertEquals("120ms", delayLabel(120))
        assertEquals("超时", delayLabel(0))
        assertNull(delayLabel(null))
    }
}
