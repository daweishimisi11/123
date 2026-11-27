package com.av123.video.data.source

import com.av123.video.data.model.Video
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jsoup 网页解析器单元测试：用内置 HTML 片段直接喂给 internal 解析入口，
 * 不发起任何网络请求（Jsoup 为纯 JVM 实现）。
 */
class JsoupParserTest {

    private val source = JsoupVideoDataSource(
        JsoupSiteConfig(
            baseUrl = "https://123av.com/",
            localePath = "/cn"
        )
    )

    private fun parse(html: String) =
        Jsoup.parse(html, "https://123av.com/cn/new")

    // ---------- 卡片 / 列表页 ----------

    @Test
    fun parseCategoryDocument_parsesCardFieldsAndAbsCoverUrl() {
        val html = """
            <div class="card">
              <a class="card__cover" href="/cn/v/siro-5733">
                <img class="card__img" src="https://cdn.example.com/covers/siro.jpg">
              </a>
              <div class="card__title"><a href="/cn/v/siro-5733">SIRO-5733 — 测试影片标题</a></div>
              <span class="card__dur">32:10</span>
              <span class="card__views">5.8K</span>
              <div class="card__meta"><span>5 分钟前</span></div>
            </div>
        """.trimIndent()

        val page = source.parseCategoryDocument(parse(html), category = "分类A", page = 1)

        assertEquals(1, page.videos.size)
        val video = page.videos.first()
        assertEquals("siro-5733", video.id)
        assertEquals("SIRO-5733", video.code)
        assertEquals("SIRO-5733 — 测试影片标题", video.title)
        assertEquals("分类A", video.category)
        // 相对 / 协议相对地址必须补成绝对地址
        assertEquals("https://cdn.example.com/covers/siro.jpg", video.coverUrl)
        assertEquals("32:10", video.durationText)
        assertEquals(5800L, video.views)
        assertEquals("/cn/v/siro-5733", video.detailPath)
        assertEquals("https://123av.com/cn/v/siro-5733", video.sourcePageUrl)
    }

    @Test
    fun parseCategoryDocument_parsesPaginationFromLastLink() {
        val html = """
            <div class="card">
              <a class="card__cover" href="/cn/v/a-1"><div class="card__title"><a>A-1</a></div></a>
            </div>
            <div class="pager">
              <div class="pager__pages">
                <a href="/cn/new?page=1">1</a>
                <a class="is-on" href="/cn/new?page=2">2</a>
                <a rel="last" href="/cn/new?page=88">88</a>
              </div>
            </div>
        """.trimIndent()

        val page = source.parseCategoryDocument(parse(html), category = "新发布", page = 2)

        assertEquals(2, page.pagination?.currentPage)
        assertEquals(88, page.pagination?.totalPages)
    }

    @Test
    fun parseCategoryDocument_withoutPagerReturnsNullPagination() {
        val html = """
            <div class="card">
              <a class="card__cover" href="/cn/v/a-1"><div class="card__title"><a>A-1</a></div></a>
            </div>
        """.trimIndent()

        val page = source.parseCategoryDocument(parse(html), category = "x", page = 1)
        assertNull(page.pagination)
    }

    @Test
    fun parseChannelDocument_prefersSmallerExplicitLastAndParsesCommaTotal() {
        val htmlInflatedMax = """
            <div class="card">
              <a class="card__cover" href="/cn/v/a-1"><div class="card__title"><a>A-1</a></div></a>
            </div>
            <div class="pager">
              <div class="pager__pages">
                <a class="is-on" href="/cn/genres/11?page=1">1</a>
                <a rel="last" href="/cn/genres/11?page=42">42</a>
              </div>
              <input type="number" name="page" max="5000" value="1">
              <span class="pager__total">/ 1,234</span>
            </div>
        """.trimIndent()
        val parsed = source.parseChannelDocument(parse(htmlInflatedMax), label = "标签", page = 1)
        assertEquals(42, parsed.pagination?.totalPages)

        val htmlCommaOnly = """
            <div class="card">
              <a class="card__cover" href="/cn/v/a-1"><div class="card__title"><a>A-1</a></div></a>
            </div>
            <div class="pager">
              <span class="pager__total">/ 1,234</span>
            </div>
        """.trimIndent()
        val commaParsed = source.parseChannelDocument(parse(htmlCommaOnly), label = "标签", page = 1)
        assertEquals(1234, commaParsed.pagination?.totalPages)

        val htmlLastPageSpan = """
            <div class="card">
              <a class="card__cover" href="/cn/v/a-9"><div class="card__title"><a>A-9</a></div></a>
            </div>
            <div class="pager">
              <div class="pager__pages">
                <a href="/cn/actresses/aiko?page=10">10</a>
                <a href="/cn/actresses/aiko?page=11">11</a>
                <span class="is-on">12</span>
              </div>
            </div>
        """.trimIndent()
        val lastPageParsed = source.parseChannelDocument(parse(htmlLastPageSpan), label = "演员", page = 12)
        assertEquals(12, lastPageParsed.pagination?.totalPages)
        assertEquals(12, lastPageParsed.pagination?.currentPage)
    }

    @Test
    fun parseSearchDocument_parsesCardsCountAndPagination() {
        val html = """
            <div class="pagehead">
              <span class="pagehead__count">1,234 个视频</span>
            </div>
            <div class="card">
              <a class="card__cover" href="/cn/v/search-001">
                <img class="card__img" src="/covers/search.jpg">
              </a>
              <div class="card__title"><a href="/cn/v/search-001">SEARCH-001 — 搜索结果</a></div>
            </div>
            <div class="pager">
              <div class="pager__pages">
                <a class="is-on" href="/cn/search?q=x&page=1">1</a>
                <a rel="last" href="/cn/search?q=x&page=7">7</a>
              </div>
            </div>
        """.trimIndent()

        val page = source.parseSearchDocument(parse(html), page = 1)

        assertEquals(1, page.videos.size)
        assertEquals("search-001", page.videos.first().id)
        assertEquals("搜索", page.videos.first().category)
        assertEquals(1234, page.totalCount)
        assertEquals(7, page.pagination?.totalPages)
    }

    // ---------- 首页 ----------

    @Test
    fun parseHomeDocument_parsesFeaturedSectionsAndHotMorePath() {
        val html = """
            <nav class="nav">
              <div class="nav__item"><a class="nav__link" href="/cn/new">新发布</a></div>
            </nav>
            <div class="featured">
              <a class="featured__link" href="/cn/v/feat-001">
                <img class="featured__img" src="/uploads/feat.jpg">
                <span class="featured__title">FEAT-001 — 精选大片</span>
                <span class="featured__dur">12:34</span>
              </a>
            </div>
            <section class="section">
              <h2 class="section__title">热门视频</h2>
              <a class="section__all" href="/cn/hot">查看全部</a>
              <div class="card">
                <a class="card__cover" href="/cn/v/hot-001">
                  <img class="card__img" src="/uploads/hot.jpg">
                </a>
                <div class="card__title"><a href="/cn/v/hot-001">HOT-001 — 热门影片</a></div>
                <span class="card__views">1.5K</span>
              </div>
            </section>
        """.trimIndent()

        val feed = source.parseHomeDocument(parse(html))

        assertEquals(1, feed.featured.size)
        assertEquals("feat-001", feed.featured.first().id)
        assertEquals("精选", feed.featured.first().category)
        assertEquals(1, feed.hot.size)
        assertEquals("hot-001", feed.hot.first().id)
        assertEquals("/cn/hot", feed.hotMorePath)
        assertTrue(feed.navGroups.isNotEmpty())
    }

    // ---------- 播放器剧集配置（转义还原，最易回归） ----------

    @Test
    fun parsePlayerData_unescapesJsonSlashesAndAmpAndExtractsPoster() {
        // 注意：三引号字符串中 \u0022 / \/ / \u0026 均为字面文本（与网页源码一致）
        val html = """
            <div x-data="player(JSON.parse('[{\u0022url\u0022:\u0022https:\/\/123av.com\/e\/play?id=1\u0026poster=https:\/\/123av.com\/img\/p.jpg\u0022,\u0022name\u0022:\u0022第1集\u0022},{\u0022url\u0022:\u0022\/\/123av.com\/e\/2\u0022,\u0022name\u0022:\u0022第2集\u0022}]'), 42, 'SIRO-5733')"></div>
        """.trimIndent()

        val data = source.parsePlayerData(html)

        assertEquals("SIRO-5733", data.code)
        assertEquals(2, data.episodes.size)
        assertEquals("第1集", data.episodes[0].name)
        // \/ -> /，\u0026 -> &：多层转义必须全部还原，否则播放页路径错误
        assertEquals("https://123av.com/e/play?id=1&poster=https://123av.com/img/p.jpg",
            data.episodes[0].url)
        assertEquals("//123av.com/e/2", data.episodes[1].url)
        // poster 取自首集 url 查询参数
        assertEquals("https://123av.com/img/p.jpg", data.poster)
    }

    @Test
    fun parsePlayerData_missingConfigReturnsEmptyButKeepsCode() {
        val html = """<div>no player config here</div>"""
        val data = source.parsePlayerData(html)
        assertTrue(data.episodes.isEmpty())
        assertEquals("", data.poster)
    }

    // ---------- 详情页 ----------

    @Test
    fun parseDetailDocument_parsesTitleEpisodesStatsAndInfoChips() {
        val playerPayload = """[{\u0022url\u0022:\u0022https:\/\/123av.com\/e\/1?poster=https:\/\/123av.com\/poster.jpg\u0022,\u0022name\u0022:\u0022第1集\u0022}]"""
        val html = """
            <html><body>
            <h1 class="watch__title">SIRO-5733 详情页完整标题</h1>
            <div class="player" style="background-image:url('https://123av.com/bg.jpg')"></div>
            <div x-data="player(JSON.parse('$playerPayload'), 42, 'SIRO-5733')"></div>
            <div class="watch__metaitem">1.2万次观看</div>
            <div class="watch__info-row"><dt>演员</dt><dd><a href="/cn/actresses/aiko">Aiko</a></dd></div>
            <div class="watch__info-row"><dt>代码</dt><dd>SIRO-5733</dd></div>
            </body></html>
        """.trimIndent()

        val reference = Video(id = "siro-5733", title = "列表缓存标题", category = "")
        val detail = source.parseDetailDocument(
            Jsoup.parse(html, "https://123av.com/"),
            reference,
            resolvedUrl = "https://123av.com/cn/v/siro-5733"
        )

        requireNotNull(detail)
        assertEquals("SIRO-5733 详情页完整标题", detail.title)
        assertEquals(1, detail.episodes.size)
        assertEquals("https://123av.com/e/1?poster=https://123av.com/poster.jpg", detail.embedUrl)
        // 封面优先取 .player 背景
        assertEquals("https://123av.com/bg.jpg", detail.coverUrl)
        assertEquals(12_000L, detail.views)
        assertEquals(2, detail.infoRows.size)
        val actressRow = detail.infoRows.first { it.label == "演员" }
        assertEquals(1, actressRow.chips.size)
        assertEquals("Aiko", actressRow.chips.first().text)
        assertEquals("/cn/actresses/aiko", actressRow.chips.first().path)
        assertEquals("https://123av.com/cn/v/siro-5733", detail.sourcePageUrl)
        assertTrue(detail.alternateVersions.isEmpty())
    }

    @Test
    fun parseDetailDocument_extractsAlternateCensoredAndUncensoredVersionsOnlyFromDetailArea() {
        val html = """
            <html><body>
            <h1 class="watch__title">IPX-589 测试影片</h1>
            <div class="watch__acts">
              <a href="/cn/v/ipx-589-uncensored-leaked">无码流出</a>
            </div>
            <div class="watch__info-row"><dt>类型</dt><dd><a href="/cn/censored">有码</a></dd></div>
            <aside class="watch-side">
              <a class="vside" href="/cn/v/other-111-uncensored-leaked"><span class="vside__title">其他推荐无码</span></a>
            </aside>
            <section class="related">
              <div class="card"><a class="card__cover" href="/cn/v/other-222-uncensored-leaked"></a></div>
            </section>
            </body></html>
        """.trimIndent()

        val censoredDoc = source.parseDetailDocument(
            Jsoup.parse(html, "https://123av.com/"),
            Video(id = "ipx-589", title = "IPX-589", category = ""),
            resolvedUrl = "https://123av.com/cn/v/ipx-589"
        )
        requireNotNull(censoredDoc)
        assertEquals(1, censoredDoc.alternateVersions.size)
        val uncensored = censoredDoc.alternateVersions.first()
        assertEquals("ipx-589-uncensored-leaked", uncensored.videoId)
        assertEquals("无码流出", uncensored.label)
        assertEquals("/cn/v/ipx-589-uncensored-leaked", uncensored.path)
        // 普通分类链接 /cn/censored 仍保留在 infoRows 胶囊中
        assertEquals(1, censoredDoc.infoRows.first().chips.size)

        val reverseHtml = """
            <html><body>
            <h1 class="watch__title">IPX-589 无码版</h1>
            <div class="watch__info-row"><dt>版本</dt><dd><a href="https://123av.com/cn/v/ipx-589">有码</a></dd></div>
            </body></html>
        """.trimIndent()
        val uncensoredDoc = source.parseDetailDocument(
            Jsoup.parse(reverseHtml, "https://123av.com/"),
            Video(id = "ipx-589-uncensored-leaked", title = "IPX-589", category = ""),
            resolvedUrl = "https://123av.com/cn/v/ipx-589-uncensored-leaked"
        )
        requireNotNull(uncensoredDoc)
        assertEquals(1, uncensoredDoc.alternateVersions.size)
        val censored = uncensoredDoc.alternateVersions.first()
        assertEquals("ipx-589", censored.videoId)
        assertEquals("有码版", censored.label)
        assertEquals("/cn/v/ipx-589", censored.path)
        assertTrue(uncensoredDoc.infoRows.first().chips.isEmpty())
    }

    @Test
    fun parseDetailWithRelated_staticRelatedCardsReturnedAlongsideVideo() {
        // 同一份详情文档：详情主体 + template 原型卡（须排除）+ 静态真实推荐卡
        val html = """
            <html><body>
            <h1 class="watch__title">CUR-001 当前影片</h1>
            <div x-data="player(JSON.parse('[{&quot;url&quot;:&quot;https://123av.com/e/1&quot;,&quot;name&quot;:&quot;第1集&quot;}]'), 1, 'CUR-001')"></div>
            <template id="related-tpl">
              ${relatedCard("tpl-rec")}
            </template>
            <section class="related">
              ${relatedCard("real-rec-1")}
              ${relatedCard("real-rec-2")}
            </section>
            </body></html>
        """.trimIndent()

        val result = source.parseDetailWithRelated(
            Jsoup.parse(html, "https://123av.com/"),
            reference = Video(id = "cur-001", title = "CUR-001", category = ""),
            resolvedUrl = "https://123av.com/cn/v/cur-001"
        )

        requireNotNull(result.video)
        assertEquals("CUR-001 当前影片", result.video.title)
        // 静态推荐只取真实卡，template 原型卡被排除
        assertEquals(listOf("real-rec-1", "real-rec-2"), result.relatedStatic.map { it.id })
    }

    // ---------- 数字 / 时间工具 ----------

    @Test
    fun parseViews_handlesSuffixes() {
        assertEquals(1_500_000L, JsoupVideoDataSource.parseViews("1.5M次观看"))
        assertEquals(5800L, JsoupVideoDataSource.parseViews("5.8K"))
        assertEquals(12_000L, JsoupVideoDataSource.parseViews("1.2万"))
        assertEquals(300L, JsoupVideoDataSource.parseViews("300"))
        assertEquals(0L, JsoupVideoDataSource.parseViews(null))
        assertEquals(0L, JsoupVideoDataSource.parseViews(""))
    }

    @Test
    fun parsePublishedAgo_chineseAndEnglish() {
        val now = System.currentTimeMillis()
        val fiveMinAgo = JsoupVideoDataSource.parsePublishedAgo("5 分钟前")
        requireNotNull(fiveMinAgo)
        assertTrue(kotlin.math.abs(now - fiveMinAgo - 300_000L) < 5_000L)

        val twoDaysAgo = JsoupVideoDataSource.parsePublishedAgo("2 days ago")
        requireNotNull(twoDaysAgo)
        assertTrue(kotlin.math.abs(now - twoDaysAgo - 2L * 86_400_000L) < 5_000L)

        assertNull(JsoupVideoDataSource.parsePublishedAgo("刚刚"))
        assertNull(JsoupVideoDataSource.parsePublishedAgo(null))
    }

    // ---------- 纯 JVM URL 工具 ----------

    @Test
    fun siteUrls_queryParameter_decodesAndHandlesMissing() {
        assertEquals("2", SiteUrls.queryParameter("/cn/new?page=2", "page"))
        assertNull(SiteUrls.queryParameter("/cn/new", "page"))
        assertNull(SiteUrls.queryParameter(null, "page"))
        assertEquals("a b", SiteUrls.queryParameter("/s?q=a%20b", "q"))
        assertEquals("123av.com", SiteUrls.host("https://123av.com/cn/new"))
        assertEquals("/cn/v/1?page=3", SiteUrls.pathAndQuery("https://123av.com/cn/v/1?page=3"))
    }

    // ---------- 相关推荐 ----------

    private fun relatedCard(id: String) = """
        <div class="card">
          <a class="card__cover" href="/cn/v/$id">
            <img class="card__img" src="https://cdn.example.com/$id.jpg">
          </a>
          <div class="card__title"><a href="/cn/v/$id">$id — 推荐影片</a></div>
        </div>
    """.trimIndent()

    @Test
    fun parseRelatedHtml_parsesCardsWithBaseUrl() {
        val html = relatedCard("rec-001") + relatedCard("rec-002")

        val related = source.parseRelatedHtml(html, "https://123av.com/cn/v/current-1")

        assertEquals(listOf("rec-001", "rec-002"), related.map { it.id })
        assertEquals("https://cdn.example.com/rec-001.jpg", related.first().coverUrl)
        assertEquals("https://123av.com/cn/v/rec-001", related.first().sourcePageUrl)
    }

    @Test
    fun parseRelatedHtml_excludesCardsInsideTemplate() {
        val html = """
            <template id="more-tpl">
              ${relatedCard("tpl-001")}
            </template>
            <div>
              ${relatedCard("real-001")}
            </div>
        """.trimIndent()

        val related = source.parseRelatedHtml(html, "https://123av.com/cn/v/current-1")

        assertEquals(listOf("real-001"), related.map { it.id })
    }

    @Test
    fun parseRelatedHtml_ignoresCardWithoutCoverLink() {
        val html = """
            <div class="card">
              <div class="card__title"><a href="/cn/v/no-cover">无封面卡片</a></div>
            </div>
            ${relatedCard("real-002")}
        """.trimIndent()

        val related = source.parseRelatedHtml(html, "https://123av.com/cn/v/current-1")

        assertEquals(listOf("real-002"), related.map { it.id })
    }

    @Test
    fun parseRelatedHtml_blankInputReturnsEmpty() {
        assertTrue(source.parseRelatedHtml("", "https://123av.com/").isEmpty())
    }

    @Test
    fun parseRelatedHtml_parsesServerSideVsideEntries() {
        // 详情页服务端直出的“相关视频”侧栏：封面在 thumb 的 background-image 里，
        // meta 内依次为发布时间 / 分隔点 / 观看数
        val html = """
            <aside class="watch-side"><ul class="watch-side__list">
              <li><a class="vside" href="/cn/v/side-001">
                <span class="vside__thumb"
                      data-preview="https://icdn.123av.me/preview/side-001/preview.png"
                      style="background-image:url('https://icdn.123av.me/img/side-001/cover.jpg')">
                  <span class="vside__dur">2:06:21</span>
                </span>
                <span class="vside__body">
                  <span class="vside__title">SIDE-001 — 侧栏相关视频</span>
                  <span class="vside__meta">
                    <span>1 天 12 小时前</span>
                    <span class="dot"></span>
                    <span>18.1K次观看</span>
                  </span>
                </span>
              </a></li>
            </ul></aside>
        """.trimIndent()

        val related = source.parseRelatedHtml(html, "https://123av.com/cn/v/current-1")

        assertEquals(listOf("side-001"), related.map { it.id })
        val item = related.first()
        assertEquals("https://icdn.123av.me/img/side-001/cover.jpg", item.coverUrl)
        assertEquals("https://icdn.123av.me/preview/side-001/preview.png", item.previewUrl)
        assertEquals("2:06:21", item.durationText)
        assertEquals("1 天 12 小时前", item.publishedAgo)
        assertEquals("18.1K次观看", item.viewsText)
        assertEquals("https://123av.com/cn/v/side-001", item.sourcePageUrl)
    }

    @Test
    fun parseDetailWithRelated_sideVideosPreferredOverTemplateCards() {
        // 同时存在侧栏条目与 template 原型卡时，静态推荐取侧栏、原型卡不入选
        val html = """
            <html><body>
            <h1 class="watch__title">CUR-001 当前影片</h1>
            <div x-data="player(JSON.parse('[{&quot;url&quot;:&quot;https://123av.com/e/1&quot;,&quot;name&quot;:&quot;第1集&quot;}]'), 1, 'CUR-001')"></div>
            <section class="rec">
              <template x-for="item in items">${relatedCard("tpl-rec")}</template>
            </section>
            <aside class="watch-side"><ul class="watch-side__list">
              <li><a class="vside" href="/cn/v/side-a">
                <span class="vside__thumb" style="background-image:url('https://icdn.123av.me/a.jpg')"></span>
                <span class="vside__body"><span class="vside__title">SIDE-A</span></span>
              </a></li>
            </ul></aside>
            </body></html>
        """.trimIndent()

        val result = source.parseDetailWithRelated(
            Jsoup.parse(html, "https://123av.com/"),
            reference = Video(id = "cur-001", title = "CUR-001", category = ""),
            resolvedUrl = "https://123av.com/cn/v/cur-001"
        )

        assertEquals(listOf("side-a"), result.relatedStatic.map { it.id })
    }

    @Test
    fun dedupRelated_removesSelfDuplicatesBlankAndLimits() {
        fun v(id: String) = Video(id = id, title = id, category = "视频")
        val input = listOf(
            v("a"), v("current"), v("b"), v("a"), v(""), v("c"), v("d"), v("e")
        )

        val result = com.av123.video.data.repository.dedupRelated(
            videos = input,
            currentVideoId = "current",
            limit = 4
        )

        assertEquals(listOf("a", "b", "c", "d"), result.map { it.id })
    }

    // ---------- 演员日文名规范化与双语解析 ----------

    @Test
    fun normalizeJapaneseKana_convertsHalfWidthKanaAndDakutenToFullWidth() {
        assertEquals("白峰ミウ", normalizeJapaneseKana("白峰ﾐｳ"))
        assertEquals("シラミネ ミウ", normalizeJapaneseKana("ｼﾗﾐﾈ ﾐｳ"))
        assertEquals("ガギグゲゴ パピプペポ ヴァー・", normalizeJapaneseKana("ｶﾞｷﾞｸﾞｹﾞｺﾞ ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ ｳﾞｧｰ･"))
        assertEquals("Miu Shiramine (白峰ミウ)", formatActressBilingualName("Miu Shiramine", "白峰ﾐｳ"))
        // 日文站回退只写英文时不重复拼接
        assertEquals("Miu Shiramine", formatActressBilingualName("Miu Shiramine", "Miu Shiramine"))
    }

    @Test
    fun localeHelpers_mapChinesePathsAndUrlsToJapanese() {
        assertEquals("/ja/actresses/miu-shiramine", toJapaneseLocalePath("/cn/actresses/miu-shiramine", "/cn"))
        assertEquals(
            "https://123av.com/ja/v/ipx-683-uncensored-leaked",
            toJapaneseLocaleUrl("https://123av.com/cn/v/ipx-683-uncensored-leaked", "https://123av.com/", "/cn")
        )
        assertEquals("miu-shiramine", extractActressSlugFromPath("/cn/actresses/miu-shiramine"))
        assertNull(extractActressSlugFromPath("/cn/actresses"))
    }

    @Test
    fun parseDetailDocument_mergesNormalizedJapaneseActressNamesFromJaDoc() {
        val cnHtml = """
            <html><body>
            <h1 class="watch__title">IPX-683</h1>
            <div class="watch__info-row"><dt>演员</dt><dd><a href="/cn/actresses/miu-shiramine">Miu Shiramine</a> <a href="/cn/actresses/english-only">English Only</a></dd></div>
            <div class="watch__info-row"><dt>类型</dt><dd><a href="/cn/genres/11">Drama</a></dd></div>
            </body></html>
        """.trimIndent()
        val jaHtml = """
            <html><body>
            <h1 class="watch__title">IPX-683</h1>
            <div class="watch__info-row"><dt>女優</dt><dd><a href="/ja/actresses/miu-shiramine">白峰ﾐｳ</a> <a href="/ja/actresses/english-only">English Only</a></dd></div>
            <div class="watch__info-row"><dt>ジャンル</dt><dd><a href="/ja/genres/11">ドラマ</a></dd></div>
            </body></html>
        """.trimIndent()

        val detail = source.parseDetailDocument(
            doc = Jsoup.parse(cnHtml, "https://123av.com/"),
            reference = Video(id = "ipx-683", title = "IPX-683", category = ""),
            resolvedUrl = "https://123av.com/cn/v/ipx-683",
            jaDoc = Jsoup.parse(jaHtml, "https://123av.com/")
        )

        requireNotNull(detail)
        val actressChips = detail.infoRows.first { it.label == "演员" }.chips
        assertEquals(2, actressChips.size)
        assertEquals("Miu Shiramine", actressChips[0].text)
        assertEquals("白峰ミウ", actressChips[0].japaneseName)
        assertEquals("Miu Shiramine (白峰ミウ)", actressChips[0].displayText())
        assertEquals("English Only", actressChips[1].displayText())
        // 非演员胶囊保持原名
        val genreChip = detail.infoRows.first { it.label == "类型" }.chips.first()
        assertEquals("", genreChip.japaneseName)
        assertEquals("Drama", genreChip.displayText())
    }

    @Test
    fun parseChannelDocument_mergesNormalizedJapaneseActressProfileName() {
        val cnHtml = """
            <html><body>
            <section class="ahero">
              <div class="ahero__avatar" style="background-image:url('https://icdn.123av.me/miu.jpg')"></div>
              <h1 class="ahero__name">Miu Shiramine</h1>
              <div class="ahero__meta">170cm</div>
            </section>
            </body></html>
        """.trimIndent()
        val jaHtml = """
            <html><body>
            <section class="ahero">
              <h1 class="ahero__name">白峰ﾐｳ</h1>
            </section>
            </body></html>
        """.trimIndent()

        val page = source.parseChannelDocument(
            doc = Jsoup.parse(cnHtml, "https://123av.com/"),
            label = "Miu Shiramine",
            page = 1,
            jaDoc = Jsoup.parse(jaHtml, "https://123av.com/")
        )

        val profile = requireNotNull(page.actressProfile)
        assertEquals("Miu Shiramine", profile.name)
        assertEquals("白峰ミウ", profile.japaneseName)
        assertEquals("Miu Shiramine (白峰ミウ)", profile.displayName())
    }
}
