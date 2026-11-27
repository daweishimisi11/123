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

    @Test
    fun `AppLockState relocks only after timeout and remembers prior unlock across background`() {
        val state = AppLockState(initialUnlocked = false, relockAfterMs = 30_000L)
        assertFalse(state.unlocked)
        assertFalse(state.hasUnlockedOnce)
        assertTrue(state.isLockedOrPendingRelock(nowMs = 1_000L))

        state.unlock()
        assertTrue(state.unlocked)
        assertTrue(state.hasUnlockedOnce)

        // 短暂切后台（未超时）不触发重新锁定
        state.markBackgrounded(nowMs = 10_000L)
        assertFalse(state.isLockedOrPendingRelock(nowMs = 25_000L))
        assertFalse(state.evaluateOnResume(nowMs = 25_000L))
        assertTrue(state.unlocked)

        // 离开达到或超过 30 秒后回前台触发锁定，但 hasUnlockedOnce 保留以保住页面导航栈
        state.markBackgrounded(nowMs = 30_000L)
        assertTrue(state.isLockedOrPendingRelock(nowMs = 60_000L))
        assertTrue(state.evaluateOnResume(nowMs = 60_000L))
        assertFalse(state.unlocked)
        assertTrue(state.hasUnlockedOnce)

        // 指纹解锁后恢复已解锁状态
        state.unlock()
        assertTrue(state.unlocked)
        assertTrue(state.hasUnlockedOnce)
    }
}
