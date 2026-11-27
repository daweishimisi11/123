package com.av123.video.ui.components.player

import androidx.media3.common.C
import com.av123.video.data.prefs.VideoBufferSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkAwareLoadControlTest {

    @Test
    fun `resolveBufferNetworkCategory distinguishes wifi, cellular, ethernet, and metered vpn`() {
        assertEquals(
            BufferNetworkCategory.WIFI,
            resolveBufferNetworkCategory(
                hasWifi = true,
                hasEthernet = false,
                hasCellular = false,
                isMetered = false
            )
        )
        // Wi-Fi + VPN 场景优先按 Wi-Fi 归类
        assertEquals(
            BufferNetworkCategory.WIFI,
            resolveBufferNetworkCategory(
                hasWifi = true,
                hasEthernet = false,
                hasCellular = true,
                isMetered = true
            )
        )
        assertEquals(
            BufferNetworkCategory.CELLULAR,
            resolveBufferNetworkCategory(
                hasWifi = false,
                hasEthernet = false,
                hasCellular = true,
                isMetered = true
            )
        )
        assertEquals(
            BufferNetworkCategory.WIFI,
            resolveBufferNetworkCategory(
                hasWifi = false,
                hasEthernet = true,
                hasCellular = false,
                isMetered = false
            )
        )
        // 未暴露底层传输的纯 VPN 回退到 isMetered 判断
        assertEquals(
            BufferNetworkCategory.CELLULAR,
            resolveBufferNetworkCategory(
                hasWifi = false,
                hasEthernet = false,
                hasCellular = false,
                isMetered = true
            )
        )
        assertEquals(
            BufferNetworkCategory.WIFI,
            resolveBufferNetworkCategory(
                hasWifi = false,
                hasEthernet = false,
                hasCellular = false,
                isMetered = false
            )
        )
    }

    @Test
    fun `selectActiveBufferSize picks wifi or cellular setting independently`() {
        assertEquals(
            VideoBufferSize.ULTRA,
            selectActiveBufferSize(
                category = BufferNetworkCategory.WIFI,
                wifiBufferSize = VideoBufferSize.ULTRA,
                cellularBufferSize = VideoBufferSize.SMALL
            )
        )
        assertEquals(
            VideoBufferSize.SMALL,
            selectActiveBufferSize(
                category = BufferNetworkCategory.CELLULAR,
                wifiBufferSize = VideoBufferSize.ULTRA,
                cellularBufferSize = VideoBufferSize.SMALL
            )
        )
    }

    @Test
    fun `buffer presets satisfy DefaultLoadControl ordering constraints`() {
        VideoBufferSize.entries.forEach { preset ->
            assertTrue(preset.bufferForPlaybackMs >= 0)
            assertTrue(preset.bufferForPlaybackAfterRebufferMs >= 0)
            assertTrue(preset.minBufferMs >= preset.bufferForPlaybackMs)
            assertTrue(preset.minBufferMs >= preset.bufferForPlaybackAfterRebufferMs)
            assertTrue(preset.maxBufferMs >= preset.minBufferMs)
        }
    }

    @Test
    fun `BufferLoadingEvaluator enforces hysteresis and reacts immediately to network switch`() {
        val evaluator = BufferLoadingEvaluator()
        val key = "player-1"
        evaluator.reset(key)

        // 数据流量 SMALL (min=10s, max=15s)：低于 10s 开始加载
        assertTrue(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 5_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.SMALL,
                delegateAllowsContinue = true
            )
        )
        // 10s–15s 之间保持加载直至触达上限 15s
        assertTrue(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 12_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.SMALL,
                delegateAllowsContinue = true
            )
        )
        // 达到 15s 停止加载
        assertFalse(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 15_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.SMALL,
                delegateAllowsContinue = true
            )
        )
        // 滞回区间 12s 仍保持停止，不频繁启停请求
        assertFalse(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 12_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.SMALL,
                delegateAllowsContinue = true
            )
        )
        // 突然切换到 Wi-Fi LARGE (min=40s, max=60s)：12s < 40s 立即恢复缓冲
        assertTrue(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 12_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.LARGE,
                delegateAllowsContinue = true
            )
        )
        // 达到 60s 后停止
        assertFalse(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 60_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.LARGE,
                delegateAllowsContinue = true
            )
        )
        // 底层 OOM/配额拒绝时强制停止
        assertFalse(
            evaluator.shouldContinueLoading(
                playerKey = key,
                bufferedDurationUs = 2_000_000L,
                playbackSpeed = 1f,
                bufferSize = VideoBufferSize.LARGE,
                delegateAllowsContinue = false
            )
        )
    }

    @Test
    fun `BufferLoadingEvaluator checks playback and rebuffer start thresholds`() {
        val evaluator = BufferLoadingEvaluator()
        assertFalse(
            evaluator.shouldStartPlayback(
                bufferedDurationUs = 1_000_000L,
                playbackSpeed = 1f,
                rebuffering = false,
                targetLiveOffsetUs = C.TIME_UNSET,
                bufferSize = VideoBufferSize.SMALL
            )
        )
        assertTrue(
            evaluator.shouldStartPlayback(
                bufferedDurationUs = 1_500_000L,
                playbackSpeed = 1f,
                rebuffering = false,
                targetLiveOffsetUs = C.TIME_UNSET,
                bufferSize = VideoBufferSize.SMALL
            )
        )
        assertFalse(
            evaluator.shouldStartPlayback(
                bufferedDurationUs = 2_000_000L,
                playbackSpeed = 1f,
                rebuffering = true,
                targetLiveOffsetUs = C.TIME_UNSET,
                bufferSize = VideoBufferSize.LARGE
            )
        )
        assertTrue(
            evaluator.shouldStartPlayback(
                bufferedDurationUs = 5_000_000L,
                playbackSpeed = 1f,
                rebuffering = true,
                targetLiveOffsetUs = C.TIME_UNSET,
                bufferSize = VideoBufferSize.LARGE
            )
        )
    }
}
