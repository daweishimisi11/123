package com.av123.video.ui.components.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.model.Episode
import com.av123.video.data.prefs.ResizeModeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsSheet_excludesPlaybackSpeedAndEpisodeControls() {
        var bufferToggledTo: Boolean? = null
        composeRule.setContent {
            SettingsSheet(
                resizeMode = ResizeModeOption.FIT,
                onResizeModeChange = {},
                autoPlayNext = true,
                onAutoPlayNextChange = {},
                flipToExitEnabled = false,
                onFlipToExitChange = {},
                showBufferHealth = false,
                onShowBufferHealthChange = { bufferToggledTo = it },
                onShowStats = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("播放设置").assertIsDisplayed()
        composeRule.onNodeWithText("画面比例").assertIsDisplayed()
        composeRule.onNodeWithText("倍速").assertDoesNotExist()
        composeRule.onNodeWithText("1.5x").assertDoesNotExist()
        composeRule.onNodeWithText("选集").assertDoesNotExist()
        composeRule.onNodeWithText("左上角缓冲秒数").assertIsDisplayed().performClick()
        assertEquals(true, bufferToggledTo)
    }

    @Test
    fun bufferHealthBadge_displaysLabelAndSeconds() {
        composeRule.setContent {
            BufferHealthBadge(bufferSeconds = 12.4f)
        }

        composeRule.onNodeWithText("缓冲").assertIsDisplayed()
        composeRule.onNodeWithText("12.4s").assertIsDisplayed()
    }

    @Test
    fun episodeSheet_onlyListsEpisodesAndSelectsTarget() {
        var picked = -1
        var dismissed = false
        composeRule.setContent {
            EpisodeSheet(
                episodes = listOf(
                    Episode(name = "正片1", url = "https://example.com/1"),
                    Episode(name = "正片2", url = "https://example.com/2")
                ),
                selectedEpisodeIndex = 0,
                onSelectEpisode = { picked = it },
                onDismiss = { dismissed = true }
            )
        }

        composeRule.onNodeWithText("选集").assertIsDisplayed()
        composeRule.onNodeWithText("正片2").assertIsDisplayed().performClick()
        assertEquals(1, picked)
        assertTrue(dismissed)
        composeRule.onNodeWithText("播放设置").assertDoesNotExist()
        composeRule.onNodeWithText("画面比例").assertDoesNotExist()
    }

    @Test
    fun trackPopupMenu_autoDisplaysActiveResolution() {
        composeRule.setContent {
            TrackPopupMenu(
                expanded = true,
                tracks = emptyList(),
                selected = null,
                activeHeight = 720,
                onSelected = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("自动 (720p)").assertIsDisplayed()
    }

    @Test
    fun speedPopupMenu_onlyListsSpeedsAndSelectsTarget() {
        var picked = 1f
        composeRule.setContent {
            SpeedPopupMenu(
                expanded = true,
                selected = 1f,
                onSelected = { picked = it },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("1.5x").assertIsDisplayed().performClick()
        assertEquals(1.5f, picked, 0.001f)
        composeRule.onNodeWithText("播放详情").assertDoesNotExist()
    }
}
