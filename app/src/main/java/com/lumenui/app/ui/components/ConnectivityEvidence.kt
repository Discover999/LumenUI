package com.lumenui.app.ui.components

import com.lumenui.app.data.ProbeKind
import com.lumenui.app.data.ProbeResult
import com.lumenui.app.data.ProbeStatus
import com.lumenui.app.data.ProbeTarget

/** One labelled observation shown on a card's detail page. */
internal data class EvidenceLine(val label: String, val value: String)

/** How the probe reaches the platform, in the reader's terms. */
internal fun kindLabel(kind: ProbeKind): String = when (kind) {
    ProbeKind.TRACE -> "Cloudflare trace"
    ProbeKind.OPENAI -> "平台自带端点"
    ProbeKind.NETFLIX -> "片源可播性"
    ProbeKind.DISNEY -> "站点响应"
    ProbeKind.YOUTUBE -> "Premium 页面"
    ProbeKind.SPOTIFY -> "注册页跳转"
    ProbeKind.GEMINI -> "主页响应"
    ProbeKind.GITHUB -> "响应头地区"
    ProbeKind.REDDIT -> "响应头 POP"
    ProbeKind.TIKTOK -> "测速节点响应头"
    ProbeKind.SIMPLE -> "站点可达性"
    ProbeKind.HBO -> "站点 cookie 地区"
    ProbeKind.PING -> "站点可达性"
}

/**
 * Explains how a verdict was reached, using only what the probe already reported plus the
 * target's own configuration. Nothing here is inferred beyond what the result carries: a
 * criterion is described, and where the result holds no evidence for it the line says so
 * rather than guessing.
 *
 * Deliberately separate from the probe routines: it describes existing results, so it can be
 * shown retroactively and cannot change any verdict.
 */
internal fun evidenceFor(target: ProbeTarget, result: ProbeResult): List<EvidenceLine> {
    val lines = mutableListOf<EvidenceLine>()
    lines += EvidenceLine("探测方式", kindLabel(target.kind))
    lines += EvidenceLine("判定依据", criterionFor(target, result))
    result.region?.let { region ->
        val colo = result.colo?.let { " · 边缘节点 $it" }.orEmpty()
        lines += EvidenceLine("出口地区", region + colo)
    }
    result.ip?.let { ip ->
        lines += EvidenceLine("出口地址", ip + if (result.isWarp) " · WARP" else "")
    }
    val successful = result.samples.filter { it >= 0 }
    if (successful.isNotEmpty()) {
        lines += EvidenceLine("平均延迟", "${successful.sum() / successful.size}ms（${successful.size} 次成功）")
    }
    if (result.samples.isNotEmpty()) {
        lines += EvidenceLine("延迟样本", result.samples.joinToString("  ") { if (it < 0) "失败" else "${it}ms" })
    }
    target.domain.takeIf { it.isNotBlank() }?.let { domain ->
        lines += EvidenceLine("探测域名", domain)
    }
    target.url.takeIf { it.isNotBlank() }?.let { url ->
        lines += EvidenceLine("探测地址", url)
    }
    if (target.unsupportedRegions.isNotEmpty()) {
        lines += EvidenceLine(
            "受限地区",
            target.unsupportedRegions.sorted().joinToString(" ")
        )
    }
    if (target.fallbackDomains.isNotEmpty()) {
        lines += EvidenceLine("备用域名", target.fallbackDomains.joinToString(" "))
    }
    return lines
}

/**
 * The criterion that actually decided this row. Each branch mirrors the corresponding probe
 * routine, so the explanation names the same observation the code tested.
 */
private fun criterionFor(target: ProbeTarget, result: ProbeResult): String = when (target.kind) {
    ProbeKind.TRACE -> when {
        target.unsupportedRegions.isNotEmpty() &&
            result.status == ProbeStatus.BLOCKED -> "出口地区命中该平台的受限地区列表"
        target.unsupportedRegions.isNotEmpty() -> "出口地区不在受限地区列表内"
        else -> "该平台没有公认的地区判定标准，仅报告能否连通"
    }
    ProbeKind.OPENAI -> "compliance 端点报告不支持国家、iOS 端点报告检测到 VPN；仅一端反对记为受限"
    ProbeKind.NETFLIX -> when (result.status) {
        ProbeStatus.UNLOCKED -> "已授权片源可播放，目录不限于自制内容"
        ProbeStatus.LIMITED -> "仅自测片源可播放，该地区只提供自制内容"
        ProbeStatus.BLOCKED -> "片源请求被拒绝或跳转离开该片源"
        else -> "片源请求没有得到回答"
    }
    ProbeKind.DISNEY -> "响应 200 且未跳转不可用页面为可用；否则记为不可用"
    ProbeKind.YOUTUBE -> "Premium 页面出现地区不可用提示记为受限；非 200 记为不可用"
    ProbeKind.SPOTIFY -> "注册页跳转命中国家路径为可用；命中不可用页面或 403/429 记为不可用"
    ProbeKind.GEMINI -> "命中 Google 反滥用拦截页或地区不支持提示记为不可用"
    ProbeKind.GITHUB -> "200 为可用，403 为不可用，其余记为探测失败"
    ProbeKind.REDDIT -> "200 或 3xx 为可用，403 为不可用"
    ProbeKind.TIKTOK -> "测速节点返回 2xx/3xx 为可用"
    ProbeKind.SIMPLE -> "站点返回 2xx/3xx 为可用"
    ProbeKind.HBO -> "站点 cookie 中的地区在该站点提供的地区列表内为可用"
    ProbeKind.PING -> "站点能连通即可用；没有公认的地区判定标准，因此不判断解锁"
}
