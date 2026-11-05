package com.wattagam.app

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.wattagam.app.feature.capture.VisitCheckScreen
import com.wattagam.app.feature.capture.VisitCheckStatus
import com.wattagam.app.feature.capture.VisitCheckUiState
import com.wattagam.app.feature.guestbook.PublishScreen
import com.wattagam.app.feature.guestbook.PublishStatus
import com.wattagam.app.feature.guestbook.PublishUiState
import com.wattagam.app.feature.showcase.BcSample
import com.wattagam.app.feature.showcase.BcSampleScreen
import com.wattagam.app.ui.theme.WattagamTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BcScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun outsideLocationOffersRecheckWithoutWriteEntry() {
        var rechecks = 0
        compose.setContent {
            WattagamTheme {
                VisitCheckScreen(VisitCheckUiState("중앙도서관", "하늘", 1, VisitCheckStatus.Outside), {}, { rechecks++ }, {}, {}, mapContent = {})
            }
        }
        compose.onNodeWithText("방명록 작성하기").assertDoesNotExist()
        compose.onNodeWithText("현재 위치 다시 확인").performClick()
        compose.runOnIdle { assertEquals(1, rechecks) }
    }

    @Test fun uploadingDisablesDuplicateRequests() {
        var submissions = 0
        compose.setContent {
            WattagamTheme {
                PublishScreen(PublishUiState("중앙도서관", "하늘", "초안", 1, PublishStatus.Uploading), {}, {}, {}, {}, { submissions++ }, photo = { Box(it.height(10.dp)) })
            }
        }
        compose.onNodeWithText("공개 업로드 중…").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, submissions) }
    }

    @Test fun failureKeepsMessageAndEmitsRetry() {
        var retries = 0
        compose.setContent {
            WattagamTheme {
                PublishScreen(PublishUiState("중앙도서관", "하늘", "남겨둔 초안", 1, PublishStatus.Failed), {}, {}, {}, {}, { retries++ }, photo = { Box(it.height(10.dp)) })
            }
        }
        compose.onNodeWithText("남겨둔 초안").assertExists()
        compose.onNodeWithText("위치 확인하고 다시 업로드").performClick()
        compose.onNodeWithText("남겨둔 초안").assertExists()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun cameraSampleExplainsThatCaptureIsNotConnected() {
        compose.setContent { WattagamTheme { BcSampleScreen(BcSample.Camera, {}, {}) } }
        compose.onNodeWithContentDescription("사진 찍기").performClick()
        compose.onNodeWithText("연동 전 UI 미리보기").assertIsDisplayed()
    }

    @Test fun renderAllDesignStatesForVisualReview() {
        val sample = mutableStateOf(BcSample.Visit)
        val compact = mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (compact.value) 1.5f else 1f)) {
                WattagamTheme { BcSampleScreen(sample.value, {}, {}, if (compact.value) Modifier.size(320.dp, 720.dp) else Modifier) }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.cacheDir, "bc-review").apply { mkdirs() }
        listOf(false, true).forEach { small ->
        compose.runOnIdle { compact.value = small }
        BcSample.entries.forEach { item ->
            compose.runOnIdle { sample.value = item }
            compose.waitForIdle()
            compose.onNodeWithText("UI 샘플").assertIsDisplayed()
            if (small) {
                when (item) {
                    BcSample.Mode -> compose.onNodeWithText("일반 사진으로 찍기").performScrollTo().assertIsDisplayed()
                    BcSample.Editor -> compose.onNodeWithText("글자 추가 · 선택").performScrollTo().assertIsDisplayed()
                    BcSample.Publish -> compose.onNodeWithText("앱 사용자에게 공개").performScrollTo().assertIsDisplayed()
                    else -> Unit
                }
            }
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(output, "${item.name}${if (small) "-compact" else ""}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        }
    }
}
