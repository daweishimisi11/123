package com.av123.video.ui.components.player

import android.media.AudioManager
import androidx.compose.ui.Alignment
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
