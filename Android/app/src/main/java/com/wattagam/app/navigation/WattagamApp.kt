package com.wattagam.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wattagam.app.BuildConfig
import com.wattagam.app.DebugCrashDialog
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.auth.LocalProfile
import com.wattagam.app.data.draft.DraftStore
import com.wattagam.app.data.location.CurrentLocationProvider
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.map.KakaoMapSupport
import com.wattagam.app.data.sample.SampleData
import com.wattagam.app.data.sample.SampleGuestbook
import com.wattagam.app.data.sample.SamplePlace
import com.wattagam.app.feature.auth.LocationPermissionRoute
import com.wattagam.app.feature.auth.LoginRoute
import com.wattagam.app.feature.auth.NicknameScreen
import com.wattagam.app.feature.gallery.GalleryScreen
import com.wattagam.app.feature.gallery.GalleryUiState
import com.wattagam.app.feature.gallery.GuestbookDetailScreen
import com.wattagam.app.feature.gallery.GuestbookDetailUiState
import com.wattagam.app.feature.map.MapPlaceUiState
import com.wattagam.app.feature.map.MapPoint
import com.wattagam.app.feature.map.MapScreen
import com.wattagam.app.feature.map.MapUiState
import com.wattagam.app.feature.mypage.MyGuestbookUiState
import com.wattagam.app.feature.mypage.MyPageScreen
import com.wattagam.app.feature.mypage.MyPageUiState
import com.wattagam.app.feature.mypage.SettingsScreen
import com.wattagam.app.feature.place.PlaceDetailScreen
import com.wattagam.app.feature.place.PlaceDetailUiState
import com.wattagam.app.feature.place.VisitCheckRoute
import com.wattagam.app.feature.place.rememberVisitCheck
import com.wattagam.app.feature.showcase.BcSample
import com.wattagam.app.feature.showcase.BcSampleIndex
import com.wattagam.app.feature.showcase.BcSampleScreen
import com.wattagam.app.feature.showcase.ShowcaseScreen
import com.wattagam.app.feature.showcase.ShowcaseSection
import com.wattagam.app.feature.write.WriteArRoute
import com.wattagam.app.feature.write.WriteCameraRoute
import com.wattagam.app.feature.write.WriteEditorRoute
import com.wattagam.app.feature.write.WriteModeRoute
import com.wattagam.app.feature.write.WritePreviewRoute
import com.wattagam.app.feature.write.WritePublishRoute
import com.wattagam.app.ui.components.GuestbookCardUiState
import com.wattagam.app.ui.components.MainTab
import com.wattagam.app.ui.components.StampUiState
import kotlinx.coroutines.launch
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
@Serializable private data class VisitCheck(val placeId: String) : NavKey         // 04, 04-E
@Serializable private data class WriteMode(val placeId: String) : NavKey          // 05
@Serializable private data class WriteAr(val placeId: String) : NavKey            // 05-A
@Serializable private data class WriteCamera(val placeId: String) : NavKey        // 05-B
@Serializable private data class WritePreview(val placeId: String) : NavKey       // 06
@Serializable private data class WriteEditor(val placeId: String) : NavKey        // 06-A
@Serializable private data class WritePublish(val placeId: String) : NavKey       // 07
@Serializable private data class GuestbookDetail(val guestbookId: String) : NavKey // 09-A-1
@Serializable private data object MyPage : NavKey                                 // 09-B
@Serializable private data object Settings : NavKey                               // 09-B-3

/**
 * 시작은 개발용 부품 목록. '앱 화면 흐름 보기'로 샘플 데이터 화면 흐름에 들어간다.
 * 카카오 로그인·카카오맵(앱 키가 있을 때), GPS 확인, 촬영·갤러리·꾸미기·초안 저장은 실제로 동작한다.
 * 서버·업로드·도장은 연결 전이며 장소·기록 화면 상태는 [SampleData] 에서 만든다.
 */
@Composable
fun WattagamApp() {
    val backStack = rememberNavBackStack(ShowcaseHome)
    // 샘플 단계에서 닉네임 수정 결과를 화면에 반영하기 위한 작은 상태 (계정 API 연결 시 대체)
    var nickname by rememberSaveable { mutableStateOf(SampleData.NICKNAME) }
    val context = LocalContext.current
    val drafts = remember { DraftStore(context) }
    val profile = remember { LocalProfile(context) }
    val locationProvider = remember { CurrentLocationProvider(context) }
    val scope = rememberCoroutineScope()
    val stampCount = SampleData.places.count { it.stamped }
    if (BuildConfig.DEBUG) DebugCrashDialog()

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
                    // 개발용: 학교 밖에서도 촬영을 시험할 수 있게 위치 확인 없이 첫 장소의 촬영 화면으로 간다 (디버그 빌드에만, 등록 단계는 위치를 다시 확인)
                    onCaptureTest = if (BuildConfig.DEBUG) {
                        { ar -> SampleData.places.first().id.let { backStack.add(if (ar) WriteAr(it) else WriteCamera(it)) } }
                    } else {
                        null
                    },
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
                LoginRoute(
                    onLoggedIn = {
                        // 이 기기에 닉네임이 저장돼 있으면(이전에 가입을 마쳤으면) 바로 지도로, 아니면 닉네임부터
                        scope.launch {
                            val saved = profile.nickname()
                            if (saved != null) {
                                nickname = saved
                                backStack.startFlowAt(MapHome)
                            } else {
                                backStack.add(Nickname(isEdit = false))
                            }
                        }
                    },
                )
            }
            entry<Nickname> { key ->
                NicknameScreen(
                    isEdit = key.isEdit,
                    initialNickname = if (key.isEdit) nickname else "",
                    onBack = { backStack.removeLastOrNull() },
                    onSubmit = {
                        nickname = it
                        scope.launch { profile.saveNickname(it) }
                        if (key.isEdit) backStack.removeLastOrNull() else backStack.add(LocationPermission)
                    },
                )
            }
            entry<LocationPermission> {
                LocationPermissionRoute(onBack = { backStack.removeLastOrNull() }, onDone = { backStack.startFlowAt(MapHome) })
            }
            entry<MapHome> {
                // 지도를 열 때 한 번 현재 위치를 재서 내 위치 점을 찍는다 (권한을 허용한 경우만, 따로 요청하지 않음)
                val myLocation by produceState<MapPoint?>(null) {
                    if (KakaoMapSupport.isAvailable) {
                        val fix = locationProvider.current()
                        if (fix is LocationFix.Found) value = MapPoint(fix.latitude, fix.longitude)
                    }
                }
                MapScreen(
                    uiState = sampleMapState(),
                    onPlaceClick = { backStack.add(PlaceDetail(it)) },
                    onTabSelect = { backStack.selectTab(it) },
                    useKakaoMap = KakaoMapSupport.isAvailable,
                    myLocation = myLocation,
                )
            }
            entry<PlaceDetail> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                val visitCheck = rememberVisitCheck(place.latitude, place.longitude, onAllowed = { backStack.add(VisitCheck(place.id)) })
                PlaceDetailScreen(
                    uiState = place.toDetailState(),
                    onBack = { backStack.removeLastOrNull() },
                    onGalleryClick = { backStack.add(Gallery(key.placeId)) },
                    onWriteClick = visitCheck.start,
                    locationStatus = visitCheck.status,
                )
            }
            entry<Gallery> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                val visitCheck = rememberVisitCheck(place.latitude, place.longitude, onAllowed = { backStack.add(VisitCheck(place.id)) })
                GalleryScreen(
                    uiState = sampleGalleryState(place, nickname),
                    onBack = { backStack.removeLastOrNull() },
                    onPlaceInfoClick = {
                        // 장소 설명에서 왔으면 뒤로, 아니면 새로 연다
                        if (backStack.getOrNull(backStack.size - 2) == PlaceDetail(key.placeId)) backStack.removeLastOrNull()
                        else backStack.add(PlaceDetail(key.placeId))
                    },
                    onWriteClick = visitCheck.start,
                    onGuestbookClick = { backStack.add(GuestbookDetail(it)) },
                    locationStatus = visitCheck.status,
                )
            }
            entry<VisitCheck> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                VisitCheckRoute(
                    placeName = place.name,
                    visitorName = nickname,
                    stampCount = stampCount,
                    placeOrder = place.order,
                    placeStamped = place.stamped,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    onBack = { backStack.removeLastOrNull() },
                    onContinue = { backStack.add(WriteMode(place.id)) },
                    onMap = { backStack.selectTab(MainTab.Map) },
                )
            }
            entry<WriteMode> { key ->
                WriteModeRoute(
                    placeId = key.placeId,
                    stampCount = stampCount,
                    drafts = drafts,
                    onBack = { backStack.removeLastOrNull() },
                    onAr = { backStack.add(WriteAr(key.placeId)) },
                    onPhoto = { backStack.add(WriteCamera(key.placeId)) },
                    onResumeDraft = { backStack.add(WritePublish(key.placeId)) },
                )
            }
            entry<WriteAr> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                WriteArRoute(
                    placeId = place.id,
                    placeName = place.name,
                    drafts = drafts,
                    onBack = { backStack.removeLastOrNull() },
                    onSwitchToPhoto = {
                        backStack.removeLastOrNull()
                        backStack.add(WriteCamera(place.id))
                    },
                    onPhotoReady = { backStack.add(WritePreview(place.id)) },
                )
            }
            entry<WriteCamera> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                WriteCameraRoute(
                    placeId = place.id,
                    placeName = place.name,
                    drafts = drafts,
                    onBack = { backStack.removeLastOrNull() },
                    onPhotoReady = { backStack.add(WritePreview(place.id)) },
                )
            }
            entry<WritePreview> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                WritePreviewRoute(
                    placeId = place.id,
                    placeName = place.name,
                    drafts = drafts,
                    onBack = { backStack.removeLastOrNull() },
                    // AR 로 찍었으면 AR 촬영으로, 아니면 일반 촬영으로 돌아간다
                    onRetake = {
                        if (backStack.getOrNull(backStack.lastIndex - 1) == WriteAr(place.id)) backStack.removeLastOrNull()
                        else backStack.backOrAdd(WriteCamera(place.id))
                    },
                    onEdit = { backStack.add(WriteEditor(place.id)) },
                    onNext = { backStack.add(WritePublish(place.id)) },
                )
            }
            entry<WriteEditor> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                WriteEditorRoute(
                    placeId = place.id,
                    placeName = place.name,
                    drafts = drafts,
                    onBack = { backStack.removeLastOrNull() },
                    // 업로드 화면에서 '수정'으로 왔다면 예전 업로드 화면을 치우고 새 사진으로 다시 연다
                    onDone = {
                        backStack.remove(WritePublish(place.id))
                        backStack.add(WritePublish(place.id))
                    },
                )
            }
            entry<WritePublish> { key ->
                val place = SampleData.place(key.placeId) ?: return@entry
                WritePublishRoute(
                    placeId = place.id,
                    placeName = place.name,
                    authorName = nickname,
                    stampCount = stampCount,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    drafts = drafts,
                    onBack = { backStack.removeLastOrNull() },
                    onEditPhoto = { backStack.backOrAdd(WriteEditor(place.id)) },
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
                    onLogout = {
                        KakaoLogin.logout()
                        scope.launch { profile.clear() }
                        backStack.startFlowAt(Login)
                    },
                    // 탈퇴 API 연결 전: 지금은 카카오 토큰과 이 기기의 닉네임만 지우고 로그인으로 돌아간다
                    onWithdraw = {
                        KakaoLogin.logout()
                        scope.launch { profile.clear() }
                        backStack.startFlowAt(Login)
                    },
                )
            }
        },
    )
}

/** 바로 앞 화면이 [key] 면 그리로 돌아가고, 아니면 새로 연다 (다시 찍기·사진 편집으로 돌아가기) */
private fun MutableList<NavKey>.backOrAdd(key: NavKey) {
    if (getOrNull(size - 2) == key) removeAt(lastIndex) else add(key)
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
    places = SampleData.places.map { MapPlaceUiState(it.id, it.order, it.name, it.checkpoint, it.stamped, it.latitude, it.longitude) },
)

private fun SamplePlace.toDetailState() = PlaceDetailUiState(
    SampleData.COURSE, order, name, description, highlight, writingPrompt,
    directions, checkpoint, stamped, SampleData.guestbooksAt(id).size,
)

private fun SampleGuestbook.card(nickname: String) =
    GuestbookCardUiState(id, title, "${if (mine) nickname else author} · ${createdAt.substringBeforeLast(' ')}")

private fun sampleGalleryState(place: SamplePlace, nickname: String) =
    GalleryUiState(place.name, SampleData.guestbooksAt(place.id).map { it.card(nickname) })

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
