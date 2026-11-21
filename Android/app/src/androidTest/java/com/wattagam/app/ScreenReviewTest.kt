package com.wattagam.app

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.wattagam.app.data.sample.SampleData
import com.wattagam.app.feature.auth.LoginScreen
import com.wattagam.app.feature.gallery.GalleryScreen
import com.wattagam.app.feature.gallery.GalleryUiState
import com.wattagam.app.feature.map.MapPlaceUiState
import com.wattagam.app.feature.map.MapScreen
import com.wattagam.app.feature.map.MapUiState
import com.wattagam.app.feature.mypage.MyGuestbookUiState
import com.wattagam.app.feature.mypage.MyPageScreen
import com.wattagam.app.feature.mypage.MyPageUiState
import com.wattagam.app.feature.mypage.SettingsScreen
import com.wattagam.app.feature.place.PlaceDetailScreen
import com.wattagam.app.feature.place.PlaceDetailUiState
import com.wattagam.app.feature.showcase.BcSample
import com.wattagam.app.feature.showcase.BcSampleScreen
import com.wattagam.app.ui.components.GuestbookCardUiState
import com.wattagam.app.ui.components.LocationUiState
import com.wattagam.app.ui.components.StampUiState
import com.wattagam.app.ui.theme.WattagamTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

/**
 * 화면 검토용: 주요 화면을 그림 파일로 남긴다 (검사가 아니라 눈으로 비교하기 위한 자료).
 * 연결된 기기 검사를 돌리면 build/outputs/connected_android_test_additional_output 아래로 복사된다.
 */
class ScreenReviewTest {
    @get:Rule val compose = createComposeRule()

    private val places = SampleData.places
    private val cards = places.mapIndexed { i, place -> GuestbookCardUiState("g$i", "${place.name}에서 남긴 오후", "하늘 · 2026. 10. 2${i + 1}.") }

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "A-01-login" to @Composable { LoginScreen(onKakaoLogin = {}, sampleMode = false) },
        "A-02-map" to @Composable {
            MapScreen(
                MapUiState("푸른캠퍼스 산책", places.map { MapPlaceUiState(it.id, it.order, it.name, it.checkpoint, it.stamped, it.latitude, it.longitude) }),
                onPlaceClick = {}, onTabSelect = {},
            )
        },
        "A-03-place" to @Composable {
            val place = places[2]
            PlaceDetailScreen(
                PlaceDetailUiState("푸른캠퍼스 산책", place.order, place.name, place.description, place.highlight, place.writingPrompt, place.directions, place.checkpoint, place.stamped, 3),
                onBack = {}, onGalleryClick = {}, onWriteClick = {}, locationStatus = LocationUiState.Outside,
            )
        },
        "A-03-place-stamped" to @Composable {
            val place = places[0]
            PlaceDetailScreen(
                PlaceDetailUiState("푸른캠퍼스 산책", place.order, place.name, place.description, place.highlight, place.writingPrompt, place.directions, place.checkpoint, place.stamped, 3),
                onBack = {}, onGalleryClick = {}, onWriteClick = {},
            )
        },
        "D-gallery" to @Composable { GalleryScreen(GalleryUiState("도서관", cards), onBack = {}, onPlaceInfoClick = {}, onWriteClick = {}, onGuestbookClick = {}) },
        "E-mypage" to @Composable {
            MyPageScreen(
                MyPageUiState("하늘", places.map { StampUiState(it.id, it.name, it.stamped) }, places.zip(cards) { p, c -> MyGuestbookUiState(p.id, c) }),
                onSettingsClick = {}, onGuestbookClick = {}, onTabSelect = {},
            )
        },
        "E-settings" to @Composable { SettingsScreen("하늘", 2, onBack = {}, onEditNickname = {}, onLogout = {}, onWithdraw = {}) },
    ) + BcSample.entries.map { sample -> "BC-${sample.ordinal}-${sample.name}" to @Composable { BcSampleScreen(sample, {}, {}) } }

    @Test fun renderScreensForReview() {
        val current = mutableStateOf(screens.first().second)
        compose.setContent { WattagamTheme { current.value() } }
        val arguments = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(arguments.getString("additionalTestOutputDir") ?: context.cacheDir.path, "screen-review").apply { mkdirs() }
        screens.forEach { (name, screen) ->
            compose.runOnIdle { current.value = screen }
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
