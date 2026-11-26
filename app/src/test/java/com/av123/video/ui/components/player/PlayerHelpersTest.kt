package com.av123.video.ui.components.player

import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class PlayerHelpersTest {

    @Test
    fun `parseVideoTracks sorts descending and keeps exact mediaTrackGroup`() {
        val audioGroup = TrackGroup(
            "audio",
            Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AAC).build()
        )
        val textGroup = TrackGroup(
            "text",
            Format.Builder().setSampleMimeType(MimeTypes.TEXT_VTT).build()
        )
        val videoGroup = TrackGroup(
            "video",
            Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setHeight(720).setBitrate(2_000_000).build(),
            Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setHeight(1080).setBitrate(5_000_000).build()
        )
        val tracks = Tracks(
            listOf(
                Tracks.Group(audioGroup, false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(true)),
                Tracks.Group(textGroup, false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(false)),
                Tracks.Group(
                    videoGroup,
                    true,
                    intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
                    booleanArrayOf(false, true)
                )
            )
        )

        val parsed = parseVideoTracks(tracks)
        assertEquals(2, parsed.size)
        assertEquals(listOf(1080, 720), parsed.map { it.height })
        assertEquals(listOf(1, 0), parsed.map { it.trackIndex })
        assertTrue(parsed.all { it.mediaTrackGroup == videoGroup })
    }

    @Test
    fun `buildTrackSelectionParameters overrides exact video group and clears only video`() {
        val audioGroup = TrackGroup(
            "audio",
            Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AAC).build()
        )
        val videoGroup = TrackGroup(
            "video",
            Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setHeight(720).build(),
            Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setHeight(1080).build()
        )
        val tracks = Tracks(
            listOf(
                Tracks.Group(audioGroup, false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(true)),
                Tracks.Group(
                    videoGroup,
                    true,
                    intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
                    booleanArrayOf(true, false)
                )
            )
        )
        val target1080 = parseVideoTracks(tracks).first { it.height == 1080 }

        val baseParams = TrackSelectionParameters.DEFAULT
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(audioGroup, listOf(0)))
            .build()

        val selectedParams = buildTrackSelectionParameters(baseParams, target1080)
        assertEquals(listOf(target1080.trackIndex), selectedParams.overrides[videoGroup]?.trackIndices)
        assertEquals(listOf(0), selectedParams.overrides[audioGroup]?.trackIndices)

        val autoParams = buildTrackSelectionParameters(selectedParams, null)
        assertNull(autoParams.overrides[videoGroup])
        assertEquals(listOf(0), autoParams.overrides[audioGroup]?.trackIndices)
    }

    @Test
    fun `shouldReResolveOnError triggers re-resolve for 4xx except 429 or repeated failures`() {
        assertTrue(shouldReResolveOnError(httpStatusCode = 403, retryAttempts = 0))
        assertTrue(shouldReResolveOnError(httpStatusCode = 404, retryAttempts = 0))
        assertFalse(shouldReResolveOnError(httpStatusCode = 429, retryAttempts = 0))
        assertFalse(shouldReResolveOnError(httpStatusCode = 502, retryAttempts = 0))
        assertFalse(
            shouldReResolveOnError(
                error = PlaybackException(
                    "network glitch",
                    null,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                ),
                retryAttempts = 0
            )
        )
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
}
