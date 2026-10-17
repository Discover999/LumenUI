package com.lumenui.app.ui

import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.IpReputationApi
import com.lumenui.app.data.SpeedEgress
import com.lumenui.app.data.SpeedHistoryEntry
import com.lumenui.app.data.SpeedPhase
import com.lumenui.app.data.SpeedProvider
import com.lumenui.app.data.SpeedSample
import com.lumenui.app.data.SpeedTester
import com.lumenui.app.data.TransferStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedTestRunnerTest {

    @Test
    fun start_runsEveryPhaseAndKeepsTheResult() = runTest {
        val reputation = FakeReputation()
        val finished = mutableListOf<SpeedHistoryEntry>()
        val runner = SpeedTestRunner(FakeTester(), reputation, backgroundScope) { finished += it }

        runner.start()
        testScheduler.runCurrent()

        val state = runner.state.value
        assertEquals(SpeedPhase.DONE, state.phase)
        assertEquals(55L, state.latencyMs)
        assertEquals(7.5, state.jitterMs!!, 0.001)
        assertEquals(80.0, state.downloadMbps!!, 0.001)
        assertEquals(40.0, state.uploadMbps!!, 0.001)
        assertEquals("1.1.1.1", state.egress?.ip)
        assertEquals(listOf("1.1.1.1"), reputation.lookedUp)
        assertEquals(1, state.downloadSamples.size)
        assertEquals(1, state.uploadSamples.size)
        assertEquals(SpeedProvider.CLOUDFLARE, state.provider)
        assertFalse(state.uploadUnavailable)
        assertEquals(1, finished.size)
        assertEquals(80.0, finished.first().downloadMbps!!, 0.001)
        assertEquals("1.1.1.1", finished.first().egressIp)
    }

    @Test
    fun start_ovhFallbackKeepsTheDownloadAndMarksUploadUnavailable() = runTest {
        val tester = FakeTester().apply {
            provider = SpeedProvider.OVH
            uploadResult = null
        }
        val runner = SpeedTestRunner(tester, FakeReputation(), backgroundScope)

        runner.start()
        testScheduler.runCurrent()

        val state = runner.state.value
        assertEquals(SpeedPhase.DONE, state.phase)
        assertEquals(SpeedProvider.OVH, state.provider)
        assertEquals(80.0, state.downloadMbps!!, 0.001)
        assertNull(state.uploadMbps)
        assertTrue(state.uploadUnavailable)
    }

    @Test
    fun pings_duringTheRunAreAveragedIntoTheResult() = runTest {
        val tester = FakeTester().apply { downloadDelayMs = 2_500 }
        val runner = SpeedTestRunner(tester, FakeReputation(), backgroundScope)

        runner.start()
        testScheduler.advanceTimeBy(2_600)
        testScheduler.runCurrent()

        val state = runner.state.value
        assertEquals(SpeedPhase.DONE, state.phase)
        assertEquals(53L, state.latencyMs)
        assertEquals(5.0, state.jitterMs!!, 0.001)
    }

    @Test
    fun start_failureMarksFailed() = runTest {
        val tester = FakeTester().apply { failDownload = true }
        val runner = SpeedTestRunner(tester, FakeReputation(), backgroundScope)

        runner.start()
        testScheduler.runCurrent()

        assertEquals(SpeedPhase.FAILED, runner.state.value.phase)
    }

    @Test
    fun cancel_returnsToIdle() = runTest {
        val runner = SpeedTestRunner(FakeTester(), FakeReputation(), backgroundScope)
        runner.start()
        testScheduler.runCurrent()
        assertEquals(SpeedPhase.DONE, runner.state.value.phase)

        runner.cancel()
        assertEquals(SpeedPhase.IDLE, runner.state.value.phase)
    }

    private class FakeTester : SpeedTester {
        var failDownload = false
        var downloadDelayMs = 0L
        var uploadResult: TransferStats? = TransferStats(5_000_000, 1000)
        override var provider: SpeedProvider = SpeedProvider.CLOUDFLARE

        override suspend fun latencySamples(count: Int): List<Long> = listOf(50L, 60L, 55L)

        override suspend fun ping(): Long = 50L

        override suspend fun download(
            durationMs: Long,
            onSample: (SpeedSample) -> Unit
        ): TransferStats {
            if (failDownload) error("boom")
            if (downloadDelayMs > 0) delay(downloadDelayMs)
            onSample(SpeedSample(250, 80.0))
            return TransferStats(10_000_000, 1000)
        }

        override suspend fun upload(
            durationMs: Long,
            onSample: (SpeedSample) -> Unit
        ): TransferStats? {
            val result = uploadResult ?: return null
            onSample(SpeedSample(250, 40.0))
            return result
        }

        override suspend fun egress(): SpeedEgress = SpeedEgress("1.1.1.1", "HK", "HKG")
    }

    private class FakeReputation : IpReputationApi {
        val lookedUp = mutableListOf<String>()

        override suspend fun lookup(ip: String): IpReputation? {
            lookedUp += ip
            return IpReputation(status = "success")
        }
    }
}
