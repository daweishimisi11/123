package com.av123.video.ui.screens.channels

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.model.ActressProfile
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActressHeroCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actressHeroCard_omitsTopRightFollowersLabelCapsuleAndUsesBottomFollowButton() {
        var toggled = false
        val profile = ActressProfile(
            name = "超长测试演员名字用于确认不会被右侧胶囊挤占",
            videos = "320",
            views = "1.2M",
            followers = "16,291",
            debut = "2020",
            followersLabel = "16.3K"
        )

        composeRule.setContent {
            MaterialTheme {
                ActressHeroCard(
                    profile = profile,
                    isFollowed = false,
                    followEnabled = true,
                    onToggleFollow = { toggled = true }
                )
            }
        }

        composeRule.onNodeWithText("超长测试演员名字用于确认不会被右侧胶囊挤占").assertIsDisplayed()
        composeRule.onNodeWithText("16,291").assertIsDisplayed()
        composeRule.onNodeWithText("16.3K").assertDoesNotExist()
        composeRule.onNodeWithText("关注").assertIsDisplayed().performClick()
        assertTrue(toggled)
    }
}
