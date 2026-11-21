package com.wattagam.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.auth.LocalProfile
import com.wattagam.app.ui.theme.WattagamTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppFlowTest {
    @get:Rule val compose = createComposeRule()

    /**
     * 샘플 흐름 테스트: 카카오 앱 키가 있으면 실제 카카오 로그인 창이 떠서 이 테스트로는 확인할 수 없다.
     * 이전 실행에서 저장한 닉네임이 있으면 닉네임 화면을 건너뛰므로 지운다.
     */
    @Before fun setUp() {
        assumeFalse("카카오 앱 키가 있으면 샘플 로그인 흐름을 건너뜀", KakaoLogin.isConfigured)
        runBlocking { LocalProfile(InstrumentationRegistry.getInstrumentation().targetContext).clear() }
    }

    @Test fun sampleFlowReachesNicknameAndBackReturnsToLogin() {
        compose.setContent { WattagamTheme { WattagamApp() } }
        compose.onNodeWithText("앱 화면 흐름 보기 (샘플 데이터)").performScrollTo().performClick()
        compose.onNodeWithText("카카오로 시작하기").assertIsDisplayed().performClick()
        compose.onNodeWithText("닉네임 정하기").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
    }

    @Test fun laterPermissionOpensMapAndPlaceOpensDescription() {
        compose.setContent { WattagamTheme { WattagamApp() } }
        compose.onNodeWithText("앱 화면 흐름 보기 (샘플 데이터)").performScrollTo().performClick()
        compose.onNodeWithText("카카오로 시작하기").performClick()
        compose.onNodeWithText("닉네임").performTextInput("하늘")
        compose.onNodeWithText("다음 · 위치 권한 설정").performClick()
        compose.onNodeWithText("나중에 하기 · 지도 둘러보기").performClick()
        compose.onNodeWithText("도서관 앞 계단").performScrollTo().performClick()
        compose.onNodeWithText("장소 둘러보기").assertIsDisplayed()
    }
}
