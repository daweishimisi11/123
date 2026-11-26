package com.av123.video.ui.screens.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.av123.video.data.model.VideoVersionLink
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailVersionSwitchTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun versionSwitchButton_displaysLabelAndDispatchesTarget() {
        var clicked: VideoVersionLink? = null
        val target = VideoVersionLink(
            videoId = "ipx-589-uncensored-leaked",
            label = "无码流出",
            path = "/cn/v/ipx-589-uncensored-leaked"
        )

        composeRule.setContent {
            MaterialTheme {
                DetailVersionSwitchButton(
                    version = target,
                    onClick = { clicked = target }
                )
            }
        }

        composeRule.onNodeWithText("无码流出").assertIsDisplayed().performClick()
        assertEquals(target, clicked)
    }
}
