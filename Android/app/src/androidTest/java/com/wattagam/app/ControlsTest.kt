package com.wattagam.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wattagam.app.navigation.WattagamApp
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingPreventsDuplicateSubmission() {
        var submissions = 0
        compose.setContent { WattagamTheme { WattagamButton("Upload", { submissions++ }, loading = true) } }
        compose.onNodeWithText("Upload").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, submissions) }
    }

    @Test fun enabledButtonInvokesActionOnce() {
        var submissions = 0
        compose.setContent { WattagamTheme { WattagamButton("Upload", { submissions++ }) } }
        compose.onNodeWithText("Upload").performClick()
        compose.runOnIdle { assertEquals(1, submissions) }
    }

    @Test fun showcaseNavigationReturnsToList() {
        compose.setContent { WattagamTheme { WattagamApp() } }
        compose.onNodeWithText("버튼").performClick()
        compose.onNodeWithText("방명록 남기기").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("왔다감 공통 디자인").assertIsDisplayed()
    }
}
