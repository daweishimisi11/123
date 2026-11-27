package com.av123.video.ui.components.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.model.Episode
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
    fun settingsSheet_excludesPlaybackSpeedEpisodeAndResizeControls() {
        var bufferToggledTo: Boolean? = null
        composeRule.setContent {
            SettingsSheet(
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
        composeRule.onNodeWithText("画面比例").assertDoesNotExist()
        composeRule.onNodeWithText("倍速").assertDoesNotExist()
        composeRule.onNodeWithText("1.5x").assertDoesNotExist()
        composeRule.onNodeWithText("选集").assertDoesNotExist()
        composeRule.onNodeWithText("左上角缓冲秒数").assertIsDisplayed().performClick()
        assertEquals(true, bufferToggledTo)
    }

    @Test
    fun settingsSheet_bufferSubpage_updatesWifiAndCellularSeparately() {
        var wifiPicked: com.av123.video.data.prefs.VideoBufferSize? = null
        var cellularPicked: com.av123.video.data.prefs.VideoBufferSize? = null
        composeRule.setContent {
            SettingsSheet(
                autoPlayNext = true,
                onAutoPlayNextChange = {},
                flipToExitEnabled = false,
                onFlipToExitChange = {},
                showBufferHealth = false,
                onShowBufferHealthChange = {},
                wifiBufferSize = com.av123.video.data.prefs.VideoBufferSize.LARGE,
                onWifiBufferSizeChange = { wifiPicked = it },
                cellularBufferSize = com.av123.video.data.prefs.VideoBufferSize.SMALL,
                onCellularBufferSizeChange = { cellularPicked = it },
                onShowStats = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("视频缓冲").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Wi-Fi 网络").assertIsDisplayed()
        composeRule.onNodeWithText("数据流量").assertIsDisplayed()
        composeRule.onAllNodesWithText("120 秒")[0].performClick()
        assertEquals(com.av123.video.data.prefs.VideoBufferSize.ULTRA, wifiPicked)
        composeRule.onAllNodesWithText("30 秒")[1].performClick()
        assertEquals(com.av123.video.data.prefs.VideoBufferSize.STANDARD, cellularPicked)
    }

    @Test
    fun bufferHealthBadge_displaysSecondsWithoutLabelText() {
        composeRule.setContent {
            BufferHealthBadge(bufferSeconds = 12.4f)
        }

        composeRule.onNodeWithText("12.4s").assertIsDisplayed()
        composeRule.onNodeWithText("缓冲").assertDoesNotExist()
    }

    @Test
    fun episodePopupMenu_onlyListsEpisodesAndSelectsTarget() {
        var picked = -1
        var dismissed = false
        composeRule.setContent {
            EpisodePopupMenu(
                expanded = true,
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
