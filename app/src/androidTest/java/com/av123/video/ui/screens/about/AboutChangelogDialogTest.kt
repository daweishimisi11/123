package com.av123.video.ui.screens.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutChangelogDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun changelogDialog_displaysVersionsAndHandlesActions() {
        var openedReleases = false
        var dismissed = false

        composeRule.setContent {
            MaterialTheme {
                ChangelogDialog(
                    entries = APP_CHANGELOG,
                    onOpenReleases = { openedReleases = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithText("更新日志").assertIsDisplayed()
        composeRule.onNodeWithText("v0.2.5").assertIsDisplayed()
        composeRule.onNodeWithText("v0.2.4").assertIsDisplayed()
        composeRule.onNodeWithText("全部版本").performClick()
        assertTrue(openedReleases)
        composeRule.onNodeWithText("关闭").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun aboutSheetContent_rendersCompactHeaderAndAllOptionsTogether() {
        var checkedUpdate = false
        var openedChangelog = false

        composeRule.setContent {
            MaterialTheme {
                AboutSheetContent(
                    versionName = "0.2.5",
                    checking = false,
                    onCheckUpdate = { checkedUpdate = true },
                    onOpenChangelog = { openedChangelog = true },
                    onOpenProject = {},
                    onOpenFeedback = {}
                )
            }
        }

        composeRule.onNodeWithText("v0.2.5").assertIsDisplayed()
        composeRule.onNodeWithText("检查更新").assertIsDisplayed().performClick()
        assertTrue(checkedUpdate)
        composeRule.onNodeWithText("更新日志").assertIsDisplayed().performClick()
        assertTrue(openedChangelog)
        composeRule.onNodeWithText("项目主页").assertIsDisplayed()
        composeRule.onNodeWithText("问题反馈").assertIsDisplayed()
    }
}
