package com.av123.video.ui.components.player

import android.media.AudioManager
import androidx.compose.ui.Alignment
import androidx.media3.common.Player
import com.av123.video.data.prefs.SeekGestureSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHelpersTest {

    @Test
    fun `shouldReResolveOnError triggers re-resolve for 4xx except 429 or repeated failures`() {
        assertTrue(shouldReResolveOnError(httpStatusCode = 403, retryAttempts = 0))
        assertTrue(shouldReResolveOnError(httpStatusCode = 404, retryAttempts = 0))
        assertFalse(shouldReResolveOnError(httpStatusCode = 429, retryAttempts = 0))
        assertFalse(shouldReResolveOnError(httpStatusCode = 502, retryAttempts = 0))
        assertFalse(shouldReResolveOnError(httpStatusCode = null, retryAttempts = 0))
        assertTrue(shouldReResolveOnError(httpStatusCode = null, retryAttempts = 1))
        assertTrue(shouldReResolveOnError(httpStatusCode = 502, retryAttempts = 1))
    }

    @Test
    fun `vertical gesture helpers classify halves and clamp brightness and volume`() {
        assertEquals(
            VerticalGestureSide.BRIGHTNESS,
            classifyVerticalGestureSide(startX = 100f, widthPx = 1000f)
        )
        assertEquals(
            VerticalGestureSide.VOLUME,
            classifyVerticalGestureSide(startX = 500f, widthPx = 1000f)
        )
        assertEquals(
            Alignment.CenterEnd,
            gestureOverlayAlignment(VerticalGestureSide.BRIGHTNESS)
        )
        assertEquals(
            Alignment.CenterStart,
            gestureOverlayAlignment(VerticalGestureSide.VOLUME)
        )

        val brightened = computeDraggedBrightness(
            startBrightness = 0.2f,
            accumulatedDy = -375f,
            heightPx = 1000f
        )
        assertEquals(0.7f, brightened, 0.001f)
        assertEquals(
            0.01f,
            computeDraggedBrightness(startBrightness = 0.1f, accumulatedDy = 500f, heightPx = 1000f),
            0.001f
        )
        assertEquals(
            1.0f,
            computeDraggedBrightness(startBrightness = 0.8f, accumulatedDy = -500f, heightPx = 1000f),
            0.001f
        )

        val (volUp, fracUp) = computeDraggedVolume(
            startFraction = 0.2f,
            accumulatedDy = -375f,
            heightPx = 1000f,
            maxVolume = 15
        )
        assertEquals(0.7f, fracUp, 0.001f)
        assertEquals(11, volUp)
        val (volMin, _) = computeDraggedVolume(
            startFraction = 0.1f,
            accumulatedDy = 500f,
            heightPx = 1000f,
            maxVolume = 15
        )
        assertEquals(0, volMin)
    }

    @Test
    fun `isMusicVolumeBroadcast filters music stream volume and mute events`() {
        assertTrue(isMusicVolumeBroadcast(ACTION_VOLUME_CHANGED, AudioManager.STREAM_MUSIC))
        assertTrue(isMusicVolumeBroadcast(ACTION_STREAM_MUTE_CHANGED, AudioManager.STREAM_MUSIC))
        assertTrue(isMusicVolumeBroadcast(ACTION_VOLUME_CHANGED, -1))
        assertFalse(isMusicVolumeBroadcast(ACTION_VOLUME_CHANGED, AudioManager.STREAM_RING))
        assertFalse(isMusicVolumeBroadcast("android.intent.action.OTHER", AudioManager.STREAM_MUSIC))
    }

    @Test
    fun `shouldKeepScreenOn keeps screen awake during active or fullscreen buffering playback`() {
        assertTrue(
            shouldKeepScreenOn(
                isPlaying = true,
                playWhenReady = true,
                playbackState = Player.STATE_READY,
                isFullscreen = true,
                isInPip = false,
                hasError = false
            )
        )
        assertTrue(
            shouldKeepScreenOn(
                isPlaying = false,
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING,
                isFullscreen = true,
                isInPip = false,
                hasError = false
            )
        )
        assertFalse(
            shouldKeepScreenOn(
                isPlaying = false,
                playWhenReady = false,
                playbackState = Player.STATE_READY,
                isFullscreen = true,
                isInPip = false,
                hasError = false
            )
        )
        assertFalse(
            shouldKeepScreenOn(
                isPlaying = false,
                playWhenReady = true,
                playbackState = Player.STATE_ENDED,
                isFullscreen = true,
                isInPip = false,
                hasError = false
            )
        )
        assertFalse(
            shouldKeepScreenOn(
                isPlaying = true,
                playWhenReady = true,
                playbackState = Player.STATE_READY,
                isFullscreen = true,
                isInPip = true,
                hasError = false
            )
        )
        assertFalse(
            shouldKeepScreenOn(
                isPlaying = true,
                playWhenReady = true,
                playbackState = Player.STATE_READY,
                isFullscreen = true,
                isInPip = false,
                hasError = true
            )
        )
    }

    @Test
    fun `computeHorizontalSeekTargetMs scales fine scrubbing independently of long duration`() {
        val oneHourMs = 3_600_000L
        val startMs = 600_000L

        // 默认适中档（全屏=90s）：划过 10% 屏幕只微调 9 秒，不会因为长片跳几分钟
        val standard10Pct = computeHorizontalSeekTargetMs(
            startPositionMs = startMs,
            accumulatedDx = 100f,
            widthPx = 1000f,
            durationMs = oneHourMs,
            sensitivity = SeekGestureSensitivity.STANDARD
        )
        assertEquals(609_000L, standard10Pct)

        // 精细档（全屏=60s）：划过 10% 屏幕微调 6 秒
        val fine10Pct = computeHorizontalSeekTargetMs(
            startPositionMs = startMs,
            accumulatedDx = 100f,
            widthPx = 1000f,
            durationMs = oneHourMs,
            sensitivity = SeekGestureSensitivity.FINE
        )
        assertEquals(606_000L, fine10Pct)

        // 较快档（全屏=180s）：划过半屏微调 90 秒
        val fastHalf = computeHorizontalSeekTargetMs(
            startPositionMs = startMs,
            accumulatedDx = -500f,
            widthPx = 1000f,
            durationMs = oneHourMs,
            sensitivity = SeekGestureSensitivity.FAST
        )
        assertEquals(510_000L, fastHalf)

        // 极短视频（30s）：上限钳制到视频总时长，不超出
        val shortClip = computeHorizontalSeekTargetMs(
            startPositionMs = 10_000L,
            accumulatedDx = 500f,
            widthPx = 1000f,
            durationMs = 30_000L,
            sensitivity = SeekGestureSensitivity.STANDARD
        )
        assertEquals(25_000L, shortClip)

        // 边界钳制：不超过 [0, durationMs]
        assertEquals(
            0L,
            computeHorizontalSeekTargetMs(
                startPositionMs = 5_000L,
                accumulatedDx = -800f,
                widthPx = 1000f,
                durationMs = oneHourMs,
                sensitivity = SeekGestureSensitivity.STANDARD
            )
        )
    }

    @Test
    fun `formatSeekDeltaSeconds formats signed second offsets and omits zero`() {
        assertNull(formatSeekDeltaSeconds(10_000L, 10_200L))
        assertEquals("+12s", formatSeekDeltaSeconds(60_000L, 72_000L))
        assertEquals("-8s", formatSeekDeltaSeconds(60_000L, 52_000L))
    }
}
