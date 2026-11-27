package com.av123.video.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreSpeedOptionsTest {

    @Test
    fun `parsePlaybackSpeedOptions falls back to defaults on blank or invalid input`() {
        assertEquals(DEFAULT_PLAYBACK_SPEEDS, parsePlaybackSpeedOptions(null))
        assertEquals(DEFAULT_PLAYBACK_SPEEDS, parsePlaybackSpeedOptions(""))
        assertEquals(DEFAULT_PLAYBACK_SPEEDS, parsePlaybackSpeedOptions("foo,9.9"))
    }

    @Test
    fun `parsePlaybackSpeedOptions deduplicates, filters, and sorts ascending`() {
        assertEquals(
            listOf(0.75f, 1f, 2f),
            parsePlaybackSpeedOptions("2, 1.0, 0.75, 2.0, 99")
        )
    }

    @Test
    fun `serializePlaybackSpeedOptions outputs canonical comma-separated values`() {
        assertEquals(
            "0.5,1,1.25,2",
            serializePlaybackSpeedOptions(listOf(2f, 1f, 1.25f, 0.5f))
        )
    }

    @Test
    fun `togglePlaybackSpeedOption adds, removes, and protects last remaining speed`() {
        val added = togglePlaybackSpeedOption(listOf(1f, 2f), 0.75f)
        assertEquals(listOf(0.75f, 1f, 2f), added)

        val removed = togglePlaybackSpeedOption(added, 1f)
        assertEquals(listOf(0.75f, 2f), removed)

        val single = listOf(1.5f)
        assertEquals(single, togglePlaybackSpeedOption(single, 1.5f))
    }
}
