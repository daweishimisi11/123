package com.av123.video.data.net

import android.os.SystemClock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 检查更新：优先读公开仓库的 GitHub Releases API，失败时退到 releases.atom。
 *
 * 两条都要求带 User-Agent——GitHub 对无 UA 的请求直接 403（OkHttp 不会自动补），
 * 这是之前「检查更新太容易失败」的主因；Atom 那条不受 API 的 60 次/小时限流。
 */
object UpdateChecker {

    private const val REPO = "daweishimisi11/123"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val ATOM = "https://github.com/$REPO/releases.atom"
    private const val USER_AGENT = "123AV (Android)"

    /** 同一份结果在 30 秒内复用，避免连点把限流打满 */
    private const val CACHE_WINDOW_MS = 30_000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cached: Pair<Long, Release>? = null

    /** 只要 tag 与 Release 页地址；更新说明不在应用内展示 */
    data class Release(val tag: String, val pageUrl: String)

    fun check(): Result<Release> {
        cached?.let { (at, release) ->
            if (SystemClock.elapsedRealtime() - at < CACHE_WINDOW_MS) {
                return Result.success(release)
            }
        }
        val result = runCatching { fetchApiWithRetry() }
            .recoverCatching { fetchAtom() }
        result.onSuccess { cached = SystemClock.elapsedRealtime() to it }
        return result
    }

    private fun fetchApiWithRetry(): Release {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                return fetchApi()
            } catch (t: Throwable) {
                lastError = t
                if (attempt == 0) Thread.sleep(400)
            }
        }
        throw lastError ?: IllegalStateException("update check failed")
    }

    private fun fetchApi(): Release {
        val request = Request.Builder()
            .url(API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val json = JSONObject(response.body.string())
            return Release(
                tag = json.optString("tag_name"),
                pageUrl = json.optString("html_url")
            )
        }
    }

    /** 兜底：releases.atom 的第一条就是最新版本，不受 API 限流约束 */
    private fun fetchAtom(): Release {
        val request = Request.Builder()
            .url(ATOM)
            .header("Accept", "application/atom+xml")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val xml = response.body.string()
            // 先切出第一条 entry 再在段内找链接：<title> 之前是 rel="alternate" 的 Release 页，
            // 之后是 <content> 里的 compare 链接（compare/v0.1.4...v0.1.5），
            // 直接取「title 之后的第一个 href」会跳到两版对比页
            val entry = Regex("""<entry>([\s\S]*?)</entry>""").find(xml)
                ?.groupValues?.get(1) ?: error("no release entry")
            val tag = Regex("""<title>([^<]+)</title>""").find(entry)
                ?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: error("empty tag")
            val pageUrl = Regex("""<link[^>]*rel="alternate"[^>]*href="([^"]+)"""")
                .find(entry)?.groupValues?.get(1)
                ?: Regex("""href="([^"]*/releases/tag/[^"]+)"""")
                    .find(entry)?.groupValues?.get(1)
                ?: "https://github.com/$REPO/releases/tag/$tag"
            return Release(tag = tag, pageUrl = pageUrl)
        }
    }

    private data class ParsedVersion(
        val core: List<Int>,
        val suffix: String?
    )

    private fun parseVersion(raw: String): ParsedVersion? {
        val cleaned = raw.trim().trimStart('v', 'V').substringBefore('+')
        if (cleaned.isEmpty()) return null
        val dashIndex = cleaned.indexOf('-')
        val corePart = if (dashIndex >= 0) cleaned.substring(0, dashIndex) else cleaned
        val suffixPart =
            if (dashIndex >= 0) cleaned.substring(dashIndex + 1).ifBlank { null } else null
        val coreNums = corePart.split('.').map { it.toIntOrNull() ?: return null }
        if (coreNums.isEmpty()) return null
        return ParsedVersion(coreNums, suffixPart)
    }

    private fun compareSuffix(a: String, b: String): Int {
        val partsA = a.split('.')
        val partsB = b.split('.')
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val segA = partsA.getOrNull(i) ?: return -1
            val segB = partsB.getOrNull(i) ?: return 1
            val numA = segA.toIntOrNull()
            val numB = segB.toIntOrNull()
            val cmp = when {
                numA != null && numB != null -> numA.compareTo(numB)
                else -> segA.compareTo(segB, ignoreCase = true)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    /** 比较 "v0.1.12" 与 "0.1.9" 等语义化版本串；无法解析时按「不是新版」处理 */
    fun isNewer(latest: String, current: String): Boolean {
        val a = parseVersion(latest) ?: return false
        val b = parseVersion(current) ?: return false
        for (i in 0 until maxOf(a.core.size, b.core.size)) {
            val x = a.core.getOrElse(i) { 0 }
            val y = b.core.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        val sufA = a.suffix
        val sufB = b.suffix
        return when {
            sufA == null && sufB == null -> false
            sufA == null && sufB != null -> true
            sufA != null && sufB == null -> false
            else -> compareSuffix(sufA!!, sufB!!) > 0
        }
    }
}
