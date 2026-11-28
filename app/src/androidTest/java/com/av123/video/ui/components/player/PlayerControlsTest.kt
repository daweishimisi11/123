package com.av123.video.ui.components.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.model.Episode
import org.junit.Assert.assertEquals
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
        composeRule.setContent {
            EpisodePopupMenu(
                expanded = true,
                episodes = listOf(
                    Episode(name = "正片1", url = "https://example.com/1"),
                    Episode(name = "正片2", url = "https://example.com/2")
                ),
                selectedEpisodeIndex = 0,
                onSelectEpisode = { picked = it },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("选集").assertIsDisplayed()
        composeRule.onNodeWithText("正片2").assertIsDisplayed().performClick()
        assertEquals(1, picked)
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

    @Test
    fun speedPopupMenu_respectsCustomSpeedOptions() {
        composeRule.setContent {
            SpeedPopupMenu(
                expanded = true,
                selected = 1f,
                speedOptions = listOf(1f, 2f),
                onSelected = {},
                onDismiss = {}
            )
        }

        composeRule.onAllNodesWithText("1x").assertCountEquals(2)
        composeRule.onAllNodesWithText("1x")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("1x")[1].assertIsDisplayed()
        composeRule.onNodeWithText("2x").assertIsDisplayed()
        composeRule.onNodeWithText("1.5x").assertDoesNotExist()
    }

    @Test
    fun settingsSheet_buttonsAndSpeedSubpages_invokeCallbacks() {
        var speedBtnVisible = true
        var episodeBtnVisible = true
        var trackBtnVisible = true
        var latestSpeeds = listOf(1f, 1.5f)

        composeRule.setContent {
            SettingsSheet(
                autoPlayNext = true,
                onAutoPlayNextChange = {},
                flipToExitEnabled = false,
                onFlipToExitChange = {},
                showSpeedButton = speedBtnVisible,
                onShowSpeedButtonChange = { speedBtnVisible = it },
                showEpisodeButton = episodeBtnVisible,
                onShowEpisodeButtonChange = { episodeBtnVisible = it },
                showTrackButton = trackBtnVisible,
                onShowTrackButtonChange = { trackBtnVisible = it },
                speedOptions = latestSpeeds,
                onSpeedOptionsChange = { latestSpeeds = it },
                onShowStats = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("底栏按钮").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("倍速按钮").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("选集按钮").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("清晰度按钮").assertIsDisplayed().performClick()
        assertEquals(false, speedBtnVisible)
        assertEquals(false, episodeBtnVisible)
        assertEquals(false, trackBtnVisible)

        composeRule.onNodeWithText("返回").assertDoesNotExist() // uses contentDescription
    }

    @Test
    fun abRepeatPopupMenu_setsPointsAndClears() {
        var setACount = 0
        var setBCount = 0
        var clearCount = 0
        composeRule.setContent {
            AbRepeatPopupMenu(
                expanded = true,
                abLoopState = AbLoopState(pointAMs = 12_000L, pointBMs = 45_000L),
                onSetPointA = { setACount++ },
                onSetPointB = { setBCount++ },
                onClear = { clearCount++ },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("A-B 重复").assertIsDisplayed()
        composeRule.onNodeWithText("00:12").assertIsDisplayed()
        composeRule.onNodeWithText("00:45").assertIsDisplayed()
        composeRule.onNodeWithText("设 A 点").performClick()
        composeRule.onNodeWithText("设 B 点").performClick()
        composeRule.onNodeWithText("清除重复").performClick()
        assertEquals(1, setACount)
        assertEquals(1, setBCount)
        assertEquals(1, clearCount)
    }

    @Test
    fun playerControls_hidesConfiguredBottomButtons() {
        composeRule.setContent {
            PlayerControls(
                title = "TEST-001",
                isFullscreen = true,
                isPlaying = true,
                isEnded = false,
                position = 10_000L,
                duration = 120_000L,
                bufferedPosition = 30_000L,
                selectedSpeed = 1f,
                availableTracks = emptyList(),
                selectedTrack = null,
                episodes = listOf(
                    Episode(name = "1", url = "https://example.com/1"),
                    Episode(name = "2", url = "https://example.com/2")
                ),
                selectedEpisodeIndex = 0,
                onTitleBack = {},
                onTogglePlay = {},
                onSeekBack = {},
                onSeekForward = {},
                onSeek = {},
                onSeekDragStateChange = {},
                onSpeedChange = {},
                onTrackSelected = {},
                onSelectEpisode = {},
                onControlPopupVisibleChange = {},
                onToggleFullscreen = {},
                onShowPlaybackStats = {},
                autoPlayNext = true,
                onAutoPlayNextChange = {},
                flipToExitEnabled = false,
                onFlipToExitChange = {},
                showSpeedButton = false,
                showEpisodeButton = false,
                showTrackButton = false
            )
        }

        composeRule.onNodeWithText("A-B").assertIsDisplayed()
        composeRule.onNodeWithText("1x").assertDoesNotExist()
        composeRule.onNodeWithText("选集1").assertDoesNotExist()
    }
}
