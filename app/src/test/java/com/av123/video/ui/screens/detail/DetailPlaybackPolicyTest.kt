package com.av123.video.ui.screens.detail

import com.av123.video.data.model.Video
import com.av123.video.data.model.VideoInfoRow
import org.junit.Assert.assertEquals
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
}
