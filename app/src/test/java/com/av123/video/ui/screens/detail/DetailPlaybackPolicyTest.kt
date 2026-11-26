package com.av123.video.ui.screens.detail

import com.av123.video.data.model.Video
import com.av123.video.data.model.VideoInfoRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailPlaybackPolicyTest {

    @Test
    fun `resolveDetailPlayAction queues play while resolving and starts when ready`() {
        assertEquals(
            DetailPlayClickAction.QUEUE_PENDING,
            resolveDetailPlayAction(hasPlayback = false, resolveFailed = false)
        )
        assertEquals(
            DetailPlayClickAction.RETRY_RESOLVE,
            resolveDetailPlayAction(hasPlayback = false, resolveFailed = true)
        )
        assertEquals(
            DetailPlayClickAction.START_NOW,
            resolveDetailPlayAction(hasPlayback = true, resolveFailed = false)
        )
    }

    @Test
    fun `detailHeaderTitle falls back from infoRows to code to title`() {
        val withInfoCode = Video(
            id = "1",
            title = "影片标题",
            category = "",
            code = "FALLBACK-01",
            infoRows = listOf(VideoInfoRow(label = "代码", value = "INFO-99"))
        )
        assertEquals("INFO-99", withInfoCode.detailHeaderTitle())

        val withFieldCode = Video(
            id = "2",
            title = "影片标题",
            category = "",
            code = "FIELD-02"
        )
        assertEquals("FIELD-02", withFieldCode.detailHeaderTitle())

        val titleOnly = Video(
            id = "3",
            title = "无番号自制视频标题",
            category = ""
        )
        assertEquals("无番号自制视频标题", titleOnly.detailHeaderTitle())
    }

    @Test
    fun `resolveDetailTextMetrics keeps portrait code and title compact just above 14sp synopsis`() {
        val narrow = resolveDetailTextMetrics(screenWidthDp = 360, isPortrait = true)
        val standard = resolveDetailTextMetrics(screenWidthDp = 412, isPortrait = true)
        val tablet = resolveDetailTextMetrics(screenWidthDp = 900, isPortrait = false)

        assertTrue(narrow.headerCodeFontSp in 14.5f..16f)
        assertTrue(narrow.videoTitleFontSp in 15f..16.5f)
        assertEquals(2, narrow.headerMaxLines)

        assertTrue(standard.headerCodeFontSp in 15f..16.5f)
        assertTrue(standard.videoTitleFontSp in 15.5f..17f)
        assertTrue(standard.topBarHeightDp < tablet.topBarHeightDp)
        assertTrue(standard.videoTitleFontSp <= tablet.videoTitleFontSp)
    }
}
