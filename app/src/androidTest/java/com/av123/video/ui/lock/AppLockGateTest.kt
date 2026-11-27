package com.av123.video.ui.lock

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test
    fun timeoutRelock_preservesInnerStateAndRestoresAfterUnlock() {
        val lockState = AppLockState(initialUnlocked = true, relockAfterMs = 30_000L)
        composeRule.setContent {
            AppLockGate(enabled = true, relockAfterMs = 30_000L, lockState = lockState) {
                var visits by remember { mutableIntStateOf(0) }
                Button(onClick = { visits++ }) {
                    Text("详情页计数:$visits")
                }
            }
        }

        composeRule.onNodeWithText("详情页计数:0").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("详情页计数:1").assertIsDisplayed()

        // 模拟退到后台超过超时阈值后回前台触发重锁
        composeRule.runOnUiThread {
            lockState.markBackgrounded(nowMs = 1_000L)
            lockState.evaluateOnResume(nowMs = 40_000L)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("应用已锁定").assertIsDisplayed()

        // 指纹解锁后恢复显示锁定前的内部状态（计数仍为 1，未被重建归零）
        composeRule.runOnUiThread {
            lockState.unlock()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("详情页计数:1").assertIsDisplayed()
    }
}
