package com.av123.video.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrandLogoTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun brandLogo_displays123WithoutAv() {
        composeRule.setContent {
            MaterialTheme {
                BrandLogo()
            }
        }

        composeRule.onNodeWithText("123").assertIsDisplayed()
        composeRule.onNodeWithText("AV").assertDoesNotExist()
    }
}
