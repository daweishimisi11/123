package com.av123.video.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailExpandableTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandableSynopsis_clicksTextDirectlyToExpandAndCollapse() {
        val longSynopsis = "这是一段用于测试简介文本溢出折叠与点击文字直接展开收起的长描述内容，需要超过三行才会触发截断并显示更多提示。"
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(180.dp)) {
                    ExpandableSynopsis(text = longSynopsis)
                }
            }
        }

        composeRule.onNodeWithText("更多", substring = true).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("收起", substring = true).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("更多", substring = true).assertIsDisplayed()
    }
}
