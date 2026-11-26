package com.av123.video.ui.components.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
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
}
