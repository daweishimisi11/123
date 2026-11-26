package com.av123.video.ui.screens.detail

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
}
