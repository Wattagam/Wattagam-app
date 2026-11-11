package com.wattagam.app.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wattagam.app.BuildConfig
import com.wattagam.app.DebugCrashDialog
import com.wattagam.app.R
import com.wattagam.app.data.AccountProblem
import com.wattagam.app.data.AppRepository
import com.wattagam.app.data.CampusPlaceDetail
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.draft.DraftStore
import com.wattagam.app.data.location.CurrentLocationProvider
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.map.KakaoMapSupport
import com.wattagam.app.data.sample.SampleData
import com.wattagam.app.feature.auth.LocationPermissionRoute
import com.wattagam.app.feature.auth.LoginRoute
import com.wattagam.app.feature.auth.NicknameScreen
import com.wattagam.app.feature.gallery.GuestbookDetailScreen
import com.wattagam.app.feature.gallery.GuestbookDetailUiState
import com.wattagam.app.feature.guestbook.PublishSuccessScreen
import com.wattagam.app.feature.guestbook.PublishSuccessUiState
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
import com.wattagam.app.ui.components.GuestbookPhoto
import com.wattagam.app.ui.components.LocalAccountProblemHandler
import com.wattagam.app.ui.components.MainTab
import com.wattagam.app.ui.components.RemoteContent
import com.wattagam.app.ui.components.StampUiState
import com.wattagam.app.ui.components.rememberRemote
import com.wattagam.app.ui.components.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
// 08 · 업로드 완료. 서버가 돌려준 결과를 그대로 보여주려고 값을 담는다 (초안은 이미 지웠다)
@Serializable private data class WriteDone(
    val placeId: String,
    val placeName: String,
    val imageUrl: String,
    val message: String,
    val createdAt: String,
    val stampAwarded: Boolean,
    val earned: Int,
    val total: Int,
) : NavKey
@Serializable private data class GuestbookDetail(val guestbookId: String) : NavKey // 09-A-1
@Serializable private data object MyPage : NavKey                                 // 09-B
@Serializable private data object Settings : NavKey                               // 09-B-3

/**
 * 시작은 개발용 부품 목록. '앱 화면 흐름 보기'로 앱 흐름에 들어간다.
 * 데이터는 [AppRepository]: local.properties 에 서버 주소(WATTAGAM_API_URL)와 카카오 앱 키가 있으면 서버,
 * 없으면 샘플 데이터다. 카카오 로그인·카카오맵(앱 키가 있을 때), GPS 확인, 촬영·꾸미기·초안 저장은 어느 쪽이든 실제로 동작한다.
 */
@Composable
fun WattagamApp() {
    val backStack = rememberNavBackStack(ShowcaseHome)
    val context = LocalContext.current
    val repository = remember { AppRepository.get(context) }
    // 지금 로그인한 계정의 닉네임 (로그인·닉네임 저장 때 정해진다)
    var nickname by rememberSaveable { mutableStateOf(if (repository.connectedToServer) "" else SampleData.NICKNAME) }
    val drafts = remember { DraftStore(context) }
    val locationProvider = remember { CurrentLocationProvider(context) }
    val scope = rememberCoroutineScope()
    if (BuildConfig.DEBUG) DebugCrashDialog()

    // 토큰을 다시 받을 수 없거나 닉네임이 없는 계정: 로그인부터 다시 (로그인 화면이 닉네임 화면으로 이어 준다). 초안은 지우지 않는다
    val onAccountProblem: (AccountProblem) -> Unit = { if (backStack.lastOrNull() != Login) backStack.startFlowAt(Login) }
    val back: () -> Unit = { backStack.removeLastOrNull() }

    CompositionLocalProvider(LocalAccountProblemHandler provides onAccountProblem) {
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
                            { ar ->
                                scope.launch {
                                    try {
                                        val first = repository.places().first().id
                                        backStack.add(if (ar) WriteAr(first) else WriteCamera(first))
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: AccountProblem) {
                                        onAccountProblem(e) // 서버 연결 시에는 로그인부터
                                    } catch (_: Exception) {
                                        // 장소를 받지 못하면 시험 화면으로 가지 않는다
                                    }
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
                entry<BcIndex> {
                    BcSampleIndex(onBack = back, onSelect = { backStack.add(BcDetail(it)) })
                }
                entry<BcDetail> { key ->
                    BcSampleScreen(key.sample, onBack = back, onNavigate = { backStack.add(BcDetail(it)) })
                }
                entry<ShowcaseDetail> { key ->
                    ShowcaseScreen(section = key.section, onBack = back)
                }

                entry<Login> {
                    LoginRoute(
                        repository,
                        onLoggedIn = { account ->
                            // 닉네임을 이미 정한 계정은 바로 지도로, 아니면 닉네임부터
                            val saved = account.nickname
                            if (saved != null) {
                                nickname = saved
                                backStack.startFlowAt(MapHome)
                            } else {
                                backStack.add(Nickname(isEdit = false))
                            }
                        },
                    )
                }
                entry<Nickname> { key ->
                    var submitting by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf<String?>(null) }
                    NicknameScreen(
                        isEdit = key.isEdit,
                        initialNickname = if (key.isEdit) nickname else "",
                        onBack = back,
                        onSubmit = { value ->
                            if (!submitting) {
                                submitting = true
                                scope.launch {
                                    try {
                                        nickname = repository.saveNickname(value).nickname ?: value
                                        error = null
                                        if (key.isEdit) backStack.removeLastOrNull() else backStack.add(LocationPermission)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: AccountProblem) {
                                        onAccountProblem(e)
                                    } catch (e: Exception) {
                                        error = e.userMessage(context)
                                    } finally {
                                        submitting = false
                                    }
                                }
                            }
                        },
                        submitting = submitting,
                        serverError = error,
                    )
                }
                entry<LocationPermission> {
                    LocationPermissionRoute(onBack = back, onDone = { backStack.startFlowAt(MapHome) })
                }
                entry<MapHome> {
                    // 지도를 열 때 한 번 현재 위치를 재서 내 위치 점을 찍는다 (권한을 허용한 경우만, 따로 요청하지 않음)
                    val myLocation by produceState<MapPoint?>(null) {
                        if (KakaoMapSupport.isAvailable) {
                            val fix = locationProvider.current()
                            if (fix is LocationFix.Found) value = MapPoint(fix.latitude, fix.longitude)
                        }
                    }
                    val places = rememberRemote(Unit) { repository.places() }
                    RemoteContent(places) { list ->
                        MapScreen(
                            uiState = MapUiState(
                                repository.courseName,
                                list.map { MapPlaceUiState(it.id, it.order, it.name, it.checkpoint, it.stamped, it.latitude, it.longitude) },
                            ),
                            onPlaceClick = { backStack.add(PlaceDetail(it)) },
                            onTabSelect = { backStack.selectTab(it) },
                            useKakaoMap = KakaoMapSupport.isAvailable,
                            myLocation = myLocation,
                        )
                    }
                }
                entry<PlaceDetail> { key ->
                    val detail = rememberRemote(key.placeId) { repository.placeDetail(key.placeId) }
                    RemoteContent(detail, onBack = back) { place ->
                        val visitCheck = rememberVisitCheck(
                            judge = { repository.checkVisit(key.placeId, it) },
                            onAllowed = { backStack.add(VisitCheck(key.placeId)) },
                        )
                        PlaceDetailScreen(
                            uiState = place.toUiState(repository.courseName),
                            onBack = back,
                            onGalleryClick = { backStack.add(Gallery(key.placeId)) },
                            onWriteClick = visitCheck.start,
                            locationStatus = visitCheck.status,
                        )
                    }
                }
                entry<Gallery> { key ->
                    GalleryRoute(
                        repository = repository,
                        placeId = key.placeId,
                        onBack = back,
                        onPlaceInfoClick = {
                            // 장소 설명에서 왔으면 뒤로, 아니면 새로 연다
                            if (backStack.getOrNull(backStack.size - 2) == PlaceDetail(key.placeId)) backStack.removeLastOrNull()
                            else backStack.add(PlaceDetail(key.placeId))
                        },
                        onVisitAllowed = { backStack.add(VisitCheck(key.placeId)) },
                        onGuestbookClick = { backStack.add(GuestbookDetail(it)) },
                    )
                }
                entry<VisitCheck> { key ->
                    WithPlaceAndStamps(repository, key.placeId, back) { place, stamps ->
                        VisitCheckRoute(
                            placeName = place.name,
                            visitorName = nickname,
                            stampCount = stamps.earned,
                            placeOrder = place.order,
                            placeStamped = place.stamped,
                            stampTotal = stamps.total,
                            judge = { repository.checkVisit(place.id, it) },
                            onBack = back,
                            onContinue = { backStack.add(WriteMode(place.id)) },
                            onMap = { backStack.selectTab(MainTab.Map) },
                        )
                    }
                }
                entry<WriteMode> { key ->
                    WithPlaceAndStamps(repository, key.placeId, back) { place, stamps ->
                        WriteModeRoute(
                            placeId = place.id,
                            stampCount = stamps.earned,
                            stampTotal = stamps.total,
                            drafts = drafts,
                            onBack = back,
                            onAr = { backStack.add(WriteAr(place.id)) },
                            onPhoto = { backStack.add(WriteCamera(place.id)) },
                            onResumeDraft = { backStack.add(WritePublish(place.id)) },
                        )
                    }
                }
                entry<WriteAr> { key ->
                    WithPlace(repository, key.placeId, back) { place ->
                        WriteArRoute(
                            placeId = place.id,
                            placeName = place.name,
                            drafts = drafts,
                            onBack = back,
                            onSwitchToPhoto = {
                                backStack.removeLastOrNull()
                                backStack.add(WriteCamera(place.id))
                            },
                            onPhotoReady = { backStack.add(WritePreview(place.id)) },
                        )
                    }
                }
                entry<WriteCamera> { key ->
                    WithPlace(repository, key.placeId, back) { place ->
                        WriteCameraRoute(
                            placeId = place.id,
                            placeName = place.name,
                            drafts = drafts,
                            onBack = back,
                            onPhotoReady = { backStack.add(WritePreview(place.id)) },
                        )
                    }
                }
                entry<WritePreview> { key ->
                    WithPlace(repository, key.placeId, back) { place ->
                        WritePreviewRoute(
                            placeId = place.id,
                            placeName = place.name,
                            drafts = drafts,
                            onBack = back,
                            // AR 로 찍었으면 AR 촬영으로, 아니면 일반 촬영으로 돌아간다
                            onRetake = {
                                if (backStack.getOrNull(backStack.lastIndex - 1) == WriteAr(place.id)) backStack.removeLastOrNull()
                                else backStack.backOrAdd(WriteCamera(place.id))
                            },
                            onEdit = { backStack.add(WriteEditor(place.id)) },
                            onNext = { backStack.add(WritePublish(place.id)) },
                        )
                    }
                }
                entry<WriteEditor> { key ->
                    WithPlace(repository, key.placeId, back) { place ->
                        WriteEditorRoute(
                            placeId = place.id,
                            placeName = place.name,
                            drafts = drafts,
                            onBack = back,
                            // 업로드 화면에서 '수정'으로 왔다면 예전 업로드 화면을 치우고 새 사진으로 다시 연다
                            onDone = {
                                backStack.remove(WritePublish(place.id))
                                backStack.add(WritePublish(place.id))
                            },
                        )
                    }
                }
                entry<WritePublish> { key ->
                    WithPlaceAndStamps(repository, key.placeId, back) { place, stamps ->
                        WritePublishRoute(
                            placeId = place.id,
                            placeName = place.name,
                            authorName = nickname,
                            stampCount = stamps.earned,
                            stampTotal = stamps.total,
                            judge = { repository.checkVisit(place.id, it) },
                            publish = { photo, message, fix, requestId -> repository.publish(place.id, photo, message, fix, requestId) },
                            drafts = drafts,
                            onBack = back,
                            onEditPhoto = { backStack.backOrAdd(WriteEditor(place.id)) },
                            onPublished = { done ->
                                backStack.finishWriteFlow(
                                    WriteDone(
                                        place.id, place.name, done.imageUrl, done.message, done.createdAt,
                                        done.stampAwarded, done.earnedCount, done.totalCount,
                                    ),
                                )
                            },
                        )
                    }
                }
                entry<WriteDone> { key ->
                    PublishSuccessScreen(
                        state = PublishSuccessUiState(key.placeName, nickname, key.createdAt, key.message, key.earned, key.stampAwarded, key.total),
                        onBack = back,
                        onGallery = {
                            backStack.removeLastOrNull()
                            if (backStack.lastOrNull() != Gallery(key.placeId)) backStack.add(Gallery(key.placeId))
                        },
                        onMyPage = { backStack.selectTab(MainTab.MyPage) },
                        photo = { modifier -> GuestbookPhoto(key.imageUrl, stringResource(R.string.bc_selected_photo), modifier, height = 210.dp) },
                    )
                }
                entry<GuestbookDetail> { key ->
                    val guestbook = rememberRemote(key.guestbookId) { repository.guestbook(key.guestbookId) }
                    RemoteContent(guestbook, onBack = back) { entry ->
                        GuestbookDetailScreen(
                            uiState = GuestbookDetailUiState(
                                entry.author, entry.createdAt, entry.placeName, repository.courseName, entry.message, entry.mine, entry.imageUrl,
                            ),
                            onBack = back,
                            onPlaceGalleryClick = { backStack.add(Gallery(entry.placeId)) },
                            // 신고는 서버 명세에서 빠진 기능: 샘플 화면에서만 확인 창을 보여준다
                            onReport = if (repository.connectedToServer) null else { {} },
                        )
                    }
                }
                entry<MyPage> {
                    val data = rememberRemote(Unit) {
                        coroutineScope {
                            val stamps = async { repository.stampBoard() }
                            val mine = async { repository.myGuestbooks() }
                            stamps.await() to mine.await()
                        }
                    }
                    RemoteContent(data) { (stamps, mine) ->
                        MyPageScreen(
                            uiState = MyPageUiState(
                                nickname = nickname,
                                stamps = stamps.slots.map { StampUiState(it.placeId, it.name, it.earned) },
                                guestbooks = mine.entries.map { MyGuestbookUiState(it.placeId, it.card(meta = it.placeName)) },
                            ),
                            onSettingsClick = { backStack.add(Settings) },
                            onGuestbookClick = { backStack.add(GuestbookDetail(it)) },
                            onTabSelect = { backStack.selectTab(it) },
                        )
                    }
                }
                entry<Settings> {
                    var withdrawing by remember { mutableStateOf(false) }
                    var withdrawError by remember { mutableStateOf<String?>(null) }
                    SettingsScreen(
                        nickname = nickname,
                        guestbookCount = SampleData.myGuestbooks().size,
                        onBack = back,
                        onEditNickname = { backStack.add(Nickname(isEdit = true)) },
                        onLogout = {
                            scope.launch {
                                repository.logout()
                                KakaoLogin.logout()
                                backStack.startFlowAt(Login)
                            }
                        },
                        onWithdraw = {
                            withdrawing = true
                            scope.launch {
                                try {
                                    repository.withdraw()
                                    KakaoLogin.logout()
                                    backStack.startFlowAt(Login)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: AccountProblem) {
                                    onAccountProblem(e)
                                } catch (e: Exception) {
                                    withdrawError = e.userMessage(context)
                                } finally {
                                    withdrawing = false
                                }
                            }
                        },
                        serverConnected = repository.connectedToServer,
                        withdrawing = withdrawing,
                    )
                    withdrawError?.let { message ->
                        AlertDialog(
                            onDismissRequest = { withdrawError = null },
                            title = { Text(stringResource(R.string.mypage_withdraw_failed)) },
                            text = { Text(message) },
                            confirmButton = { TextButton(onClick = { withdrawError = null }) { Text(stringResource(R.string.write_ok)) } },
                        )
                    }
                }
            },
        )
    }
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

/** 쓰기 흐름(방문 확인~업로드)을 걷어내고 [done] 을 연다. 뒤로 가면 장소 설명이나 갤러리로 돌아간다 */
private fun MutableList<NavKey>.finishWriteFlow(done: NavKey) {
    val start = indexOfFirst {
        it is VisitCheck || it is WriteMode || it is WriteAr || it is WriteCamera || it is WritePreview || it is WriteEditor || it is WritePublish
    }
    if (start >= 0) while (size > start) removeAt(lastIndex)
    add(done)
}

private fun CampusPlaceDetail.toUiState(courseName: String) = PlaceDetailUiState(
    courseName, place.order, place.name, description, highlight, writingPrompt,
    directions, place.checkpoint, place.stamped, publicCount,
)
