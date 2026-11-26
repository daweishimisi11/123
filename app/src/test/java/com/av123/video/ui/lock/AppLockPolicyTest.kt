package com.av123.video.ui.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {

    @Test
    fun `shouldRelockAfterBackground respects immediate and timed thresholds`() {
        assertTrue(shouldRelockAfterBackground(awayMs = 0L, relockAfterMs = 0L))
        assertFalse(shouldRelockAfterBackground(awayMs = 29_999L, relockAfterMs = 30_000L))
        assertTrue(shouldRelockAfterBackground(awayMs = 30_000L, relockAfterMs = 30_000L))
        assertTrue(shouldRelockAfterBackground(awayMs = 60_000L, relockAfterMs = 30_000L))
    }

    @Test
    fun `resolveAppLockToggleAction requires auth when turning off if credentials exist`() {
        assertEquals(
            AppLockToggleAction.REQUIRE_AUTH,
            resolveAppLockToggleAction(targetEnabled = false, canAuthenticate = true)
        )
        assertEquals(
            AppLockToggleAction.REQUIRE_AUTH,
            resolveAppLockToggleAction(targetEnabled = true, canAuthenticate = true)
        )
        assertEquals(
            AppLockToggleAction.DISABLE_DIRECT,
            resolveAppLockToggleAction(targetEnabled = false, canAuthenticate = false)
        )
        assertEquals(
            AppLockToggleAction.SHOW_UNAVAILABLE,
            resolveAppLockToggleAction(targetEnabled = true, canAuthenticate = false)
        )
    }
}
