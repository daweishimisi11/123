package com.av123.video.data.source

import java.text.Normalizer

private val HALF_WIDTH_KANA_RUN = Regex("""[\uFF61-\uFF9F]+""")
private val ACTRESS_DETAIL_PATH = Regex("""^(?:/[a-zA-Z]{2,5})?/actresses/([^/]+)$""")
private val LEADING_LOCALE_SEGMENT = Regex("""^/[a-zA-Z]{2,5}(?=/|$)""")

/**
 * 把半角片假名及半角日文标点（`\uFF61-\uFF9F`）规范化为全角片假名（含浊音/半浊音合成，
 * 如 `ﾐｳ` -> `ミウ`、`ｶﾞ` -> `ガ`、`ﾊﾟ` -> `パ`），并折叠多余空白。
 */
internal fun normalizeJapaneseKana(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    val halfWidthFixed = HALF_WIDTH_KANA_RUN.replace(trimmed) { match ->
        Normalizer.normalize(match.value, Normalizer.Form.NFKC)
    }
    return Normalizer.normalize(halfWidthFixed, Normalizer.Form.NFC)
        .replace(Regex("""\s+"""), " ")
        .trim()
}

/** 是否包含平假名、片假名或汉字，避免日文站回退展示纯英文时重复拼接 */
internal fun hasJapaneseScript(text: String): Boolean =
    text.any { ch ->
        val code = ch.code
        code in 0x3040..0x309F || // 平假名
            code in 0x30A0..0x30FF || // 片假名
            code in 0xFF66..0xFF9F || // 半角片假名
            code in 0x4E00..0x9FFF || // CJK 统一表意文字
            code in 0x3400..0x4DBF || // CJK 扩展 A
            code == 0x3005 // 々
    }

/** 若 [japaneseName] 为有效日文名且未重复，则返回 `英文名 (日文名)` */
internal fun formatActressBilingualName(englishName: String, japaneseName: String): String {
    val en = englishName.trim()
    val ja = normalizeJapaneseKana(japaneseName)
    if (en.isBlank()) return ja
    if (ja.isBlank() || ja.equals(en, ignoreCase = true) || !hasJapaneseScript(ja)) {
        return en
    }
    if (en.contains(ja)) return en
    return "$en ($ja)"
}

/** 从演员个人页或详情页演员链接提取演员 slug（纯 `/actresses` 索引页返回 null） */
internal fun extractActressSlugFromPath(path: String): String? {
    val clean = path.substringBefore('?').substringBefore('#').trimEnd('/')
    if (clean.isBlank()) return null
    return ACTRESS_DETAIL_PATH.find(clean)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}

/** 把同站相对路径转换为日文语种路径（`/ja/...`），保留查询串 */
internal fun toJapaneseLocalePath(path: String, currentLocalePath: String = "/cn"): String? {
    val trimmed = path.trim()
    if (trimmed.isBlank()) return null
    val queryIndex = trimmed.indexOf('?')
    val rawPath = if (queryIndex >= 0) trimmed.substring(0, queryIndex) else trimmed
    val rawQuery = if (queryIndex >= 0) trimmed.substring(queryIndex) else ""

    val normalizedPath = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
    val normalizedLocale = "/" + currentLocalePath.trim('/').lowercase()
    val jaPath = when {
        normalizedPath == "/ja" || normalizedPath.startsWith("/ja/") -> normalizedPath
        normalizedLocale.length > 1 &&
            (normalizedPath.equals(normalizedLocale, ignoreCase = true) ||
                normalizedPath.startsWith("$normalizedLocale/", ignoreCase = true)) -> {
            "/ja" + normalizedPath.substring(normalizedLocale.length)
        }
        LEADING_LOCALE_SEGMENT.containsMatchIn(normalizedPath) -> {
            LEADING_LOCALE_SEGMENT.replaceFirst(normalizedPath, "/ja")
        }
        else -> "/ja$normalizedPath"
    }
    return jaPath + rawQuery
}

/** 把同站完整 URL 或相对路径转换为对应的日文语种完整 URL */
internal fun toJapaneseLocaleUrl(
    url: String,
    baseUrl: String,
    currentLocalePath: String = "/cn"
): String? {
    val raw = url.trim()
    if (raw.isBlank()) return null
    val sitePath = if (raw.startsWith("http://", ignoreCase = true) ||
        raw.startsWith("https://", ignoreCase = true)
    ) {
        val siteHost = SiteUrls.host(baseUrl)
        val urlHost = SiteUrls.host(raw)
        if (!siteHost.isNullOrBlank() && !urlHost.equals(siteHost, ignoreCase = true)) {
            return null
        }
        SiteUrls.pathAndQuery(raw)
    } else {
        raw
    }
    val jaPath = toJapaneseLocalePath(sitePath, currentLocalePath) ?: return null
    return baseUrl.trimEnd('/') + "/" + jaPath.trimStart('/')
}
