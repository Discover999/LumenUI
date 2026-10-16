package com.lumenui.app.ui.components

import com.lumenui.app.data.ProbeKind
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the card detail page's evidence lines; they only describe what a result carries. */
class ConnectivityEvidenceTest {

    private fun target(
        kind: ProbeKind,
        name: String = "T",
        domain: String = "",
        url: String = "",
        unsupported: Set<String> = emptySet(),
        fallbacks: List<String> = emptyList()
    ) = ProbeTarget(name, "cat", kind, domain = domain, url = url, unsupportedRegions = unsupported, fallbackDomains = fallbacks)

    private fun result(
        status: ProbeStatus = ProbeStatus.UNLOCKED,
        region: String? = null,
        colo: String? = null,
        ip: String? = null,
        warp: Boolean = false,
        samples: List<Long> = emptyList()
    ) = ProbeResult("T", "cat", status, 100L, region, colo, ip, warp, samples)

    private fun value(lines: List<EvidenceLine>, label: String): String? =
        lines.firstOrNull { it.label == label }?.value

    @Test
    fun evidenceFor_namesTheProbeKindFirst() {
        val lines = evidenceFor(target(ProbeKind.TRACE), result())
        assertEquals("探测方式", lines.first().label)
        assertEquals("Cloudflare trace", lines.first().value)
    }

    @Test
    fun evidenceFor_omitsLinesTheResultHoldsNoDataFor() {
        // A reachability-only PING with no region/IP/samples must not grow placeholder rows.
        val lines = evidenceFor(target(ProbeKind.PING, url = "https://x.com/"), result())
        assertEquals(null, value(lines, "出口地区"))
        assertEquals(null, value(lines, "出口地址"))
        assertEquals(null, value(lines, "平均延迟"))
        assertEquals(null, value(lines, "延迟样本"))
        assertEquals("https://x.com/", value(lines, "探测地址"))
    }

    @Test
    fun evidenceFor_reportsTheEgressRegionWithItsEdgeNode() {
        val lines = evidenceFor(
            target(ProbeKind.TRACE, domain = "example.com"),
            result(region = "HK", colo = "HKG", ip = "1.2.3.4")
        )
        assertEquals("HK · 边缘节点 HKG", value(lines, "出口地区"))
        assertEquals("1.2.3.4", value(lines, "出口地址"))
        assertEquals("example.com", value(lines, "探测域名"))
    }

    @Test
    fun evidenceFor_flagsWarpOnTheExitAddress() {
        val lines = evidenceFor(target(ProbeKind.OPENAI), result(ip = "1.2.3.4", warp = true))
        assertEquals("1.2.3.4 · WARP", value(lines, "出口地址"))
    }

    @Test
    fun evidenceFor_averagesOnlyTheSuccessfulSamples() {
        val lines = evidenceFor(
            target(ProbeKind.SIMPLE, url = "https://x.com/"),
            result(samples = listOf(100L, -1L, 300L))
        )
        // Two of three landed, so the mean is over those two and the count says so.
        assertEquals("200ms（2 次成功）", value(lines, "平均延迟"))
        assertEquals("100ms  失败  300ms", value(lines, "延迟样本"))
    }

    @Test
    fun evidenceFor_listsTheUnsupportedRegionsOnlyWhenTheTargetHasThem() {
        val withList = evidenceFor(
            target(ProbeKind.TRACE, unsupported = setOf("CN", "HK")),
            result()
        )
        assertEquals("CN HK", value(withList, "受限地区"))

        val without = evidenceFor(target(ProbeKind.TRACE), result())
        assertEquals(null, value(without, "受限地区"))
    }

    @Test
    fun criterion_traceDistinguishesARegionBlockFromAReachabilityOnlyTarget() {
        val regionBlocked = evidenceFor(
            target(ProbeKind.TRACE, unsupported = setOf("CN")),
            result(status = ProbeStatus.BLOCKED, region = "CN")
        )
        assertTrue(value(regionBlocked, "判定依据")!!.contains("受限地区列表"))

        // No region set: the probe makes no unlock claim, and the text must not imply one.
        val noCriterion = evidenceFor(target(ProbeKind.TRACE), result())
        assertTrue(value(noCriterion, "判定依据")!!.contains("仅报告能否连通"))
    }

    @Test
    fun criterion_netflixSeparatesUnlockedFromLimitedByWhichTitlePlayed() {
        val unlocked = evidenceFor(target(ProbeKind.NETFLIX), result(ProbeStatus.UNLOCKED))
        assertTrue(value(unlocked, "判定依据")!!.contains("已授权片源可播放"))

        val limited = evidenceFor(target(ProbeKind.NETFLIX), result(ProbeStatus.LIMITED))
        assertTrue(value(limited, "判定依据")!!.contains("仅自测片源可播放"))
    }

    @Test
    fun criterion_hboNamesTheCookieRegionCheck() {
        val lines = evidenceFor(target(ProbeKind.HBO, url = "https://www.hbomax.com/"), result())
        assertTrue(value(lines, "判定依据")!!.contains("cookie"))
    }

    @Test
    fun kindLabel_coversEveryProbeKind() {
        // A new ProbeKind must be given a reader-facing name rather than falling through.
        ProbeKind.entries.forEach { kind ->
            assertFalse(kindLabel(kind).isBlank())
        }
    }

    // --- Section grouping (the detail page's layout) -------------------------

    @Test
    fun sectionsOf_keepsOnlyTheLinesThePageHasNoBespokeCardFor() {
        val lines = evidenceFor(
            target(ProbeKind.TRACE, domain = "chat.openai.com"),
            result(region = "US", colo = "HKG", ip = "1.2.3.4", samples = listOf(120L, 140L))
        )
        val sections = sectionsOf(lines)
        val labels = sections.flatMap { section -> section.lines.map { it.label } }

        // The verdict hero and the 出口 / 延迟 figure cards render these off the result itself,
        // so they must not also turn up as labelled text rows underneath.
        assertEquals(listOf("目标"), sections.map { it.title })
        listOf(
            LABEL_PROBE_KIND, LABEL_CRITERION, LABEL_REGION,
            LABEL_EXIT_ADDRESS, LABEL_AVG_LATENCY, LABEL_SAMPLES
        ).forEach { consumed ->
            assertFalse("$consumed should be rendered by the page, not a section", labels.contains(consumed))
        }
        assertTrue(labels.contains("探测域名"))
    }

    @Test
    fun sectionsOf_omitsSectionsWithNothingInThem() {
        // Reachability only: no region, no IP, no samples, so only the target section survives.
        val lines = evidenceFor(target(ProbeKind.PING, url = "https://x.com/"), result())
        assertEquals(listOf("目标"), sectionsOf(lines).map { it.title })
    }

    @Test
    fun sectionsOf_filesAnUnmappedLabelUnderOther() {
        // A label added to evidenceFor without a section must still reach the page.
        val sections = sectionsOf(listOf(EvidenceLine("新的观察", "值")))
        assertEquals(listOf("其他"), sections.map { it.title })
        assertEquals("值", sections.single().lines.single().value)
    }
}
