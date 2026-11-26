package com.av123.video.ui.screens.detail

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailFullscreenBackTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backPressExitsFullscreenBeforePoppingDetailEvenWhenPlayerUnmounted() {
        var isFullscreen by mutableStateOf(true)
        var poppedDetail by mutableStateOf(false)
        var dispatcherOwner: androidx.activity.OnBackPressedDispatcherOwner? = null

        composeRule.setContent {
            dispatcherOwner = LocalOnBackPressedDispatcherOwner.current
            BackHandler(enabled = true) { poppedDetail = true }
            FullscreenBackFallback(
                isFullscreen = isFullscreen,
                onExitFullscreen = { isFullscreen = false }
            )
        }

        composeRule.runOnIdle {
            dispatcherOwner!!.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(isFullscreen)
        assertFalse(poppedDetail)

        composeRule.runOnIdle {
            dispatcherOwner!!.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertTrue(poppedDetail)
    }
}
