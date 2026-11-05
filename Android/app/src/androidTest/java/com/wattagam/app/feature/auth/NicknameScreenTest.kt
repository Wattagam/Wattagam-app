package com.wattagam.app.feature.auth

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wattagam.app.ui.theme.WattagamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class NicknameScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tooShortNicknameCannotBeSubmitted() {
        var submitted: String? = null
        compose.setContent {
            WattagamTheme { NicknameScreen(isEdit = false, initialNickname = "하", onBack = {}, onSubmit = { submitted = it }) }
        }
        compose.onNodeWithText("다음 · 위치 권한 설정").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertNull(submitted) }
    }

    @Test fun validNicknameIsSubmittedOnce() {
        val submitted = mutableListOf<String>()
        compose.setContent {
            WattagamTheme { NicknameScreen(isEdit = false, initialNickname = "하늘", onBack = {}, onSubmit = { submitted += it }) }
        }
        compose.onNodeWithText("다음 · 위치 권한 설정").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf("하늘"), submitted) }
    }
}
