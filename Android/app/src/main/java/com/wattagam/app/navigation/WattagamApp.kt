package com.wattagam.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wattagam.app.data.sample.SampleData
import com.wattagam.app.data.sample.SampleGuestbook
import com.wattagam.app.feature.auth.LocationPermissionRoute
import com.wattagam.app.feature.auth.LoginScreen
import com.wattagam.app.feature.auth.NicknameScreen
import com.wattagam.app.feature.gallery.GalleryScreen
import com.wattagam.app.feature.gallery.GalleryUiState
import com.wattagam.app.feature.gallery.GuestbookDetailScreen
import com.wattagam.app.feature.gallery.GuestbookDetailUiState
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
import com.wattagam.app.feature.showcase.BcSampleIndex
import com.wattagam.app.feature.showcase.BcSampleScreen
import com.wattagam.app.feature.showcase.ShowcaseScreen
import com.wattagam.app.feature.showcase.ShowcaseSection
import com.wattagam.app.ui.components.GuestbookCardUiState
import com.wattagam.app.ui.components.MainTab
import com.wattagam.app.ui.components.StampUiState
import kotlinx.serialization.Serializable

@Serializable
private data object ShowcaseHome : NavKey

@Serializable
private data class ShowcaseDetail(val section: ShowcaseSection) : NavKey

@Serializable
private data object BcIndex : NavKey

@Serializable
private data class BcDetail(val sample: BcSample) : NavKey

// 앱 화면 경로. 화면 사이에는 ID 만 넘긴다. 피그마 번호를 주석으로 둔다.
@Serializable private data object Login : NavKey                                  // 01
@Serializable private data class Nickname(val isEdit: Boolean) : NavKey           // 01-A
@Serializable private data object LocationPermission : NavKey                     // 01-B
@Serializable private data object MapHome : NavKey                                // 02
@Serializable private data class PlaceDetail(val placeId: String) : NavKey        // 03, E
@Serializable private data class Gallery(val placeId: String) : NavKey            // 03-A, 09-A
@Serializable private data class GuestbookDetail(val guestbookId: String) : NavKey // 09-A-1
@Serializable private data object MyPage : NavKey                                 // 09-B
@Serializable private data object Settings : NavKey                               // 09-B-3

/**
 * 시작은 개발용 부품 목록. '앱 화면 흐름 보기'로 샘플 데이터 화면 흐름에 들어간다.
 * 로그인·GPS·업로드·도장은 연결 전이며 화면 상태는 [SampleData] 에서 만든다.
 */
@Composable
fun WattagamApp() {
    val backStack = rememberNavBackStack(ShowcaseHome)
    // 샘플 단계에서 닉네임 수정 결과를 화면에 반영하기 위한 작은 상태 (계정 API 연결 시 대체)
    var nickname by rememberSaveable { mutableStateOf(SampleData.NICKNAME) }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<ShowcaseHome> {
                ShowcaseScreen(
                    section = null,
                    onSection = { backStack.add(ShowcaseDetail(it)) },
                    onBcSamples = { backStack.add(BcIndex) },
                    onStartFlow = { backStack.add(Login) },
                )
            }
            entry<BcIndex> {
                BcSampleIndex(onBack = { backStack.removeLastOrNull() }, onSelect = { backStack.add(BcDetail(it)) })
            }
            entry<BcDetail> { key ->
                BcSampleScreen(key.sample, onBack = { backStack.removeLastOrNull() }, onNavigate = { backStack.add(BcDetail(it)) })
            }
            entry<ShowcaseDetail> { key ->
                ShowcaseScreen(section = key.section, onBack = { backStack.removeLastOrNull() })
            }

            entry<Login> {
                LoginScreen(onKakaoLogin = { backStack.add(Nickname(isEdit = false)) })
            }
            entry<Nickname> { key ->
                NicknameScreen(
                    isEdit = key.isEdit,
                    initialNickname = if (key.isEdit) nickname else "",
                    onBack = { backStack.removeLastOrNull() },
                    onSubmit = {
                        nickname = it
                        if (key.isEdit) backStack.removeLastOrNull() else backStack.add(LocationPermission)
                    },
                )
            }
            entry<LocationPermission> {
                LocationPermissionRoute(onBack = { backStack.removeLastOrNull() }, onDone = { backStack.startFlowAt(MapHome) })
            }
            entry<MapHome> {
                MapScreen(
                    uiState = sampleMapState(),
                    onPlaceClick = { backStack.add(PlaceDetail(it)) },
                    onTabSelect = { backStack.selectTab(it) },
                )
            }
            entry<PlaceDetail> { key ->
                val state = samplePlaceState(key.placeId) ?: return@entry
                PlaceDetailScreen(
                    uiState = state,
                    onBack = { backStack.removeLastOrNull() },
                    onGalleryClick = { backStack.add(Gallery(key.placeId)) },
                    onWriteClick = { /* B 담당 촬영·방명록 흐름 연결 예정: placeId 를 넘긴다 */ },
                )
            }
            entry<Gallery> { key ->
                val state = sampleGalleryState(key.placeId, nickname) ?: return@entry
                GalleryScreen(
                    uiState = state,
                    onBack = { backStack.removeLastOrNull() },
                    onPlaceInfoClick = {
                        // 장소 설명에서 왔으면 뒤로, 아니면 새로 연다
                        if (backStack.getOrNull(backStack.size - 2) == PlaceDetail(key.placeId)) backStack.removeLastOrNull()
                        else backStack.add(PlaceDetail(key.placeId))
                    },
                    onWriteClick = { /* B 담당 촬영·방명록 흐름 연결 예정 */ },
                    onGuestbookClick = { backStack.add(GuestbookDetail(it)) },
                )
            }
            entry<GuestbookDetail> { key ->
                val guestbook = SampleData.guestbook(key.guestbookId) ?: return@entry
                GuestbookDetailScreen(
                    uiState = guestbook.toDetailState(nickname),
                    onBack = { backStack.removeLastOrNull() },
                    onPlaceGalleryClick = { backStack.add(Gallery(guestbook.placeId)) },
                    onReport = { /* 신고 API 연결 예정 */ },
                )
            }
            entry<MyPage> {
                MyPageScreen(
                    uiState = sampleMyPageState(nickname),
                    onSettingsClick = { backStack.add(Settings) },
                    onGuestbookClick = { backStack.add(GuestbookDetail(it)) },
                    onTabSelect = { backStack.selectTab(it) },
                )
            }
            entry<Settings> {
                SettingsScreen(
                    nickname = nickname,
                    guestbookCount = SampleData.myGuestbooks().size,
                    onBack = { backStack.removeLastOrNull() },
                    onEditNickname = { backStack.add(Nickname(isEdit = true)) },
                    onLogout = { backStack.startFlowAt(Login) },
                    onWithdraw = { backStack.startFlowAt(Login) },
                )
            }
        },
    )
}

/** 개발용 부품 목록만 남기고 [key] 부터 새로 시작 (로그인 완료·로그아웃) */
private fun MutableList<NavKey>.startFlowAt(key: NavKey) {
    removeAll { it != ShowcaseHome }
    add(key)
}

/** 하단 탐색: 지도는 지도 화면까지 되돌아가고, 마이페이지는 지도 위에 한 번만 쌓는다 */
private fun MutableList<NavKey>.selectTab(tab: MainTab) {
    val mapIndex = lastIndexOf(MapHome)
    if (mapIndex < 0) return
    while (size > mapIndex + 1) removeAt(lastIndex)
    if (tab == MainTab.MyPage) add(MyPage)
}

// ---- 샘플 데이터 → 화면 상태 (API 단계에서 ViewModel 로 옮긴다) ----

private fun sampleMapState() = MapUiState(
    courseName = SampleData.COURSE,
    places = SampleData.places.map { MapPlaceUiState(it.id, it.order, it.name, it.checkpoint, it.stamped) },
)

private fun samplePlaceState(placeId: String): PlaceDetailUiState? {
    val p = SampleData.place(placeId) ?: return null
    return PlaceDetailUiState(
        SampleData.COURSE, p.order, p.name, p.description, p.highlight, p.writingPrompt,
        p.directions, p.checkpoint, p.stamped, SampleData.guestbooksAt(placeId).size,
    )
}

private fun SampleGuestbook.card(nickname: String) =
    GuestbookCardUiState(id, title, "${if (mine) nickname else author} · ${createdAt.substringBeforeLast(' ')}")

private fun sampleGalleryState(placeId: String, nickname: String): GalleryUiState? {
    val place = SampleData.place(placeId) ?: return null
    return GalleryUiState(place.name, SampleData.guestbooksAt(placeId).map { it.card(nickname) })
}

private fun SampleGuestbook.toDetailState(nickname: String) = GuestbookDetailUiState(
    author = if (mine) nickname else author,
    createdAt = createdAt,
    placeName = SampleData.place(placeId)?.name.orEmpty(),
    courseName = SampleData.COURSE,
    message = message,
    mine = mine,
)

private fun sampleMyPageState(nickname: String) = MyPageUiState(
    nickname = nickname,
    stamps = SampleData.places.map { StampUiState(it.id, it.name, it.stamped) },
    guestbooks = SampleData.myGuestbooks().map { g ->
        val placeName = SampleData.place(g.placeId)?.name.orEmpty()
        MyGuestbookUiState(g.placeId, GuestbookCardUiState(g.id, g.title, "$placeName · ${g.createdAt.substringBeforeLast(' ')}"))
    },
)
