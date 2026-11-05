package com.wattagam.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.wattagam.app.ui.theme.WattagamTheme
import org.junit.Rule
import org.junit.Test

class AppFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sampleFlowReachesNicknameAndBackReturnsToLogin() {
        compose.setContent { WattagamTheme { WattagamApp() } }
        compose.onNodeWithText("앱 화면 흐름 보기 (샘플 데이터)").performClick()
        compose.onNodeWithText("카카오로 시작하기").assertIsDisplayed().performClick()
        compose.onNodeWithText("닉네임 정하기").assertIsDisplayed()
        compose.onNodeWithText("뒤로").performClick()
        compose.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
    }

    @Test fun laterPermissionOpensMapAndPlaceOpensDescription() {
        compose.setContent { WattagamTheme { WattagamApp() } }
        compose.onNodeWithText("앱 화면 흐름 보기 (샘플 데이터)").performClick()
        compose.onNodeWithText("카카오로 시작하기").performClick()
        compose.onNodeWithText("닉네임").performTextInput("하늘")
        compose.onNodeWithText("다음 · 위치 권한 설정").performClick()
        compose.onNodeWithText("나중에 하기 · 지도 둘러보기").performClick()
        compose.onNodeWithText("도서관 앞 계단").performScrollTo().performClick()
        compose.onNodeWithText("장소 둘러보기").assertIsDisplayed()
    }
}
