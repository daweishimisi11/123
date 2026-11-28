package com.av123.video.ui.screens.channels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.model.ActressProfile
import com.av123.video.ui.screens.detail.resolveDetailTextMetrics
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun actressHeroCard_appendsJapaneseNameAfterEnglishName() {
        val profile = ActressProfile(
            name = "Miu Shiramine",
            japaneseName = "白峰ﾐｳ"
        )

        composeRule.setContent {
            MaterialTheme {
                ActressHeroCard(
                    profile = profile,
                    isFollowed = false,
                    followEnabled = false,
                    onToggleFollow = {}
                )
            }
        }

        composeRule.onNodeWithText("Miu Shiramine (白峰ミウ)").assertIsDisplayed()
    }

    @Test
    fun actressHeroCard_longBilingualNameUsesDetailTitleSizeWithoutEllipsis() {
        val profile = ActressProfile(
            name = "Very Long Actress Name Example",
            japaneseName = "とても長い女優名サンプルテスト"
        )
        var expectedFontSp = 0f

        composeRule.setContent {
            val configuration = LocalConfiguration.current
            expectedFontSp = resolveDetailTextMetrics(
                screenWidthDp = configuration.screenWidthDp,
                isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            ).videoTitleFontSp
            MaterialTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    ActressHeroCard(
                        profile = profile,
                        isFollowed = false,
                        followEnabled = false,
                        onToggleFollow = {}
                    )
                }
            }
        }

        val node = composeRule.onNodeWithText("Very Long Actress Name Example (とても長い女優名サンプルテスト)")
        node.assertIsDisplayed()

        val layouts = mutableListOf<TextLayoutResult>()
        val getTextLayout = node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
        assertTrue(getTextLayout.action?.invoke(layouts) == true)
        assertEquals(1, layouts.size)
        assertFalse(layouts.first().hasVisualOverflow)
        assertEquals(expectedFontSp, layouts.first().layoutInput.style.fontSize.value, 0.01f)
    }
}
