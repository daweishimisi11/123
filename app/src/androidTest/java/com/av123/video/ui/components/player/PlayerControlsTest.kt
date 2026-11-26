package com.av123.video.ui.components.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.prefs.ResizeModeOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsSheet_excludesPlaybackSpeedControls() {
        composeRule.setContent {
            SettingsSheet(
                resizeMode = ResizeModeOption.FIT,
                onResizeModeChange = {},
                episodes = emptyList(),
                selectedEpisodeIndex = 0,
                onSelectEpisode = {},
                autoPlayNext = true,
                onAutoPlayNextChange = {},
                flipToExitEnabled = false,
                onFlipToExitChange = {},
                onShowStats = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("播放设置").assertIsDisplayed()
        composeRule.onNodeWithText("画面比例").assertIsDisplayed()
        composeRule.onNodeWithText("倍速").assertDoesNotExist()
        composeRule.onNodeWithText("1.5x").assertDoesNotExist()
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
