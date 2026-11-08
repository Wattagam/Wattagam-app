package com.wattagam.app.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.wattagam.app.feature.capture.ArCaptureScreen
import com.wattagam.app.feature.capture.VisitCheckScreen
import com.wattagam.app.feature.capture.VisitCheckStatus
import com.wattagam.app.feature.capture.VisitCheckUiState
import com.wattagam.app.ui.theme.WattagamTheme
import org.junit.Rule
import org.junit.Test

/** 화면이 사실과 다른 안내를 하지 않는지 (이미 받은 도장, 놓기 전의 푸앙이, 장소 순서) */
class PolishCopyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stampedPlaceDoesNotSayStampIsPending() {
        compose.setContent {
            WattagamTheme {
                VisitCheckScreen(
                    VisitCheckUiState("정문", "하늘", 2, VisitCheckStatus.Available, placeOrder = 1, placeStamped = true),
                    {}, {}, {}, {}, mapContent = {},
                )
            }
        }
        compose.onNodeWithText("캠퍼스 산책 · 1번째 장소").assertIsDisplayed()
        compose.onNodeWithText("내 도장 2/4 · 정문 도장 보유").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("아직 도장은 찍히지 않았어요").assertDoesNotExist()
    }

    @Test fun arBadgeSaysPlacedOnlyAfterPlacing() {
        compose.setContent {
            WattagamTheme {
                ArCaptureScreen("정문", {}, {}, {}, {}, placed = false, background = {}, arContent = {})
            }
        }
        compose.onNodeWithText("푸앙이 배치 전 · 정문").assertIsDisplayed()
        compose.onNodeWithText("푸앙이 배치 완료 · 정문").assertDoesNotExist()
    }
}
