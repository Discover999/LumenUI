package com.lumenui.app.ui

import com.lumenui.app.data.ConnectivityProbe
import com.lumenui.app.data.IpReputation
import com.lumenui.app.data.IpReputationApi
import com.lumenui.app.data.ProbeKind
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityRunnerTest {

    @Test
    fun run_recordsEveryTargetAndClearsRunning() = runTest {
        val probe = FakeProbe(
            results = mapOf(
                "A" to result("A", ip = null),
                "B" to result("B", ip = null)
            )
        )
        val runner = ConnectivityRunner(probe, FakeReputation(), backgroundScope)

        runner.run(listOf(target("A"), target("B")))
        testScheduler.runCurrent()

        val state = runner.state.value
        assertFalse(state.running)
        assertEquals(setOf("A", "B"), state.results.keys)
    }

    @Test
    fun run_onlyProbesGivenTargetsAndStreamsTheirSamples() = runTest {
        val probe = FakeProbe(results = mapOf("A" to result("A", ip = null)))
        val runner = ConnectivityRunner(probe, FakeReputation(), backgroundScope)

        runner.run(listOf(target("A")))
        testScheduler.runCurrent()

        assertEquals(listOf("A"), probe.probed)
        assertEquals(listOf(1L, 2L), runner.state.value.samples["A"])
    }

    @Test
    fun run_queriesReputationOncePerDistinctIp() = runTest {
        val probe = FakeProbe(
            results = mapOf(
                "A" to result("A", ip = "1.1.1.1"),
                "B" to result("B", ip = "1.1.1.1"),
                "C" to result("C", ip = "2.2.2.2")
            )
        )
        val reputation = FakeReputation()
        val runner = ConnectivityRunner(probe, reputation, backgroundScope)

        runner.run(listOf(target("A"), target("B"), target("C")))
        testScheduler.runCurrent()

        assertEquals(listOf("1.1.1.1", "2.2.2.2"), reputation.lookedUp.sorted())
        assertEquals(setOf("1.1.1.1", "2.2.2.2"), runner.state.value.reputation.keys)
    }

    @Test
    fun retry_replacesOnlyThatTargetAndRestartsItsSamples() = runTest {
        val probe = FakeProbe(
            results = mapOf(
                "A" to result("A", ip = null),
                "B" to result("B", ip = null)
            )
        )
        val runner = ConnectivityRunner(probe, FakeReputation(), backgroundScope)
        runner.run(listOf(target("A"), target("B")))
        testScheduler.runCurrent()
        val untouched = runner.state.value.results["A"]

        runner.retry(target("B"))
        testScheduler.runCurrent()

        val state = runner.state.value
        assertEquals(untouched, state.results["A"])
        assertTrue(state.results.containsKey("B"))
        assertTrue(state.retrying.isEmpty())
        assertEquals(listOf(1L, 2L), state.samples["B"])
    }

    @Test
    fun results_areDiscardedAfterTtl() = runTest {
        val probe = FakeProbe(results = mapOf("A" to result("A", ip = null)))
        val runner = ConnectivityRunner(probe, FakeReputation(), backgroundScope)

        runner.run(listOf(target("A")))
        testScheduler.runCurrent()
        assertTrue(runner.state.value.results.isNotEmpty())

        testScheduler.advanceTimeBy(CONNECTIVITY_RESULT_TTL_MS + 1)
        testScheduler.runCurrent()

        assertTrue(runner.state.value.results.isEmpty())
    }

    @Test
    fun results_surviveWhileHeldAndExpireAfterTheHoldIsReleased() = runTest {
        val probe = FakeProbe(results = mapOf("A" to result("A", ip = null)))
        val runner = ConnectivityRunner(probe, FakeReputation(), backgroundScope)

        runner.setResultsHeld(true)
        runner.run(listOf(target("A")))
        testScheduler.runCurrent()

        // Several TTLs pass with a page open: the results must still be there to be read.
        testScheduler.advanceTimeBy(CONNECTIVITY_RESULT_TTL_MS * 3)
        testScheduler.runCurrent()
        assertTrue(runner.state.value.results.isNotEmpty())

        runner.setResultsHeld(false)
        testScheduler.advanceTimeBy(CONNECTIVITY_RESULT_TTL_MS + 1)
        testScheduler.runCurrent()

        assertTrue(runner.state.value.results.isEmpty())
    }

    private fun target(name: String) = ProbeTarget(name, "cat", ProbeKind.SIMPLE)

    private fun result(name: String, ip: String?) =
        ProbeResult(name, "cat", ProbeStatus.UNLOCKED, 5L, ip = ip)

    /** Returns canned results, recording which targets were probed and emitting two samples. */
    private class FakeProbe(private val results: Map<String, ProbeResult>) : ConnectivityProbe {
        val probed = mutableListOf<String>()

        override suspend fun probe(target: ProbeTarget, onSample: ((Long) -> Unit)?): ProbeResult {
            probed += target.name
            onSample?.invoke(1L)
            onSample?.invoke(2L)
            return results[target.name]
                ?: ProbeResult(target.name, target.category, ProbeStatus.FAILED, 0L)
        }
    }

    private class FakeReputation : IpReputationApi {
        val lookedUp = mutableListOf<String>()

        override suspend fun lookup(ip: String): IpReputation? {
            lookedUp += ip
            return IpReputation(status = "success")
        }
    }
}
