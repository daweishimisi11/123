package com.av123.video.ui.lock

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coldLaunchEnabled_showsLockOverlayAndHidesContent() {
        composeRule.setContent {
            AppLockGate(enabled = true, relockAfterMs = 0L) {
                Text("主界面内容")
            }
        }
        composeRule.onNodeWithText("主界面内容").assertDoesNotExist()
        composeRule.onNodeWithText("应用已锁定").assertIsDisplayed()
        composeRule.onNodeWithText("验证身份以继续观看").assertIsDisplayed()
    }

    @Test
    fun runtimeEnable_keepsContentVisibleWithoutImmediateRelockPrompt() {
        var enabled by mutableStateOf(false)
        composeRule.setContent {
            AppLockGate(enabled = enabled, relockAfterMs = 30_000L) {
                Text("主界面内容")
            }
        }
        composeRule.onNodeWithText("主界面内容").assertIsDisplayed()

        enabled = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("主界面内容").assertIsDisplayed()
    }
}
