package com.wattagam.app.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wattagam.app.R
import com.wattagam.app.data.AccountProblem
import com.wattagam.app.data.CampusPlace
import com.wattagam.app.data.GuestbookEntry
import com.wattagam.app.data.StampBoardData
import com.wattagam.app.data.WattagamRepository
import com.wattagam.app.feature.gallery.GalleryScreen
import com.wattagam.app.feature.gallery.GalleryUiState
import com.wattagam.app.feature.gallery.GuestbookDetailScreen
import com.wattagam.app.feature.gallery.GuestbookDetailUiState
import com.wattagam.app.feature.place.rememberVisitCheck
import com.wattagam.app.ui.components.GuestbookCardUiState
import com.wattagam.app.ui.components.LocalAccountProblemHandler
import com.wattagam.app.ui.components.RemoteContent
import com.wattagam.app.ui.components.rememberRemote
import com.wattagam.app.ui.components.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** 쓰기 흐름 화면들이 쓰는 장소. 이미 받아 둔 장소면 다시 불러오지 않는다 */
@Composable
internal fun WithPlace(repository: WattagamRepository, placeId: String, onBack: () -> Unit, content: @Composable (CampusPlace) -> Unit) {
    val place = rememberRemote(placeId, cached = repository.cachedPlace(placeId)) { repository.place(placeId) }
    RemoteContent(place, onBack = onBack, content = content)
}

/** 장소와 도장 수가 함께 필요한 화면 (방문 확인·촬영 방식·업로드) */
@Composable
internal fun WithPlaceAndStamps(
    repository: WattagamRepository,
    placeId: String,
    onBack: () -> Unit,
    content: @Composable (CampusPlace, StampBoardData) -> Unit,
) {
    val cachedPlace = repository.cachedPlace(placeId)
    val cachedStamps = repository.cachedStampBoard()
    val data = rememberRemote(placeId, cached = if (cachedPlace != null && cachedStamps != null) cachedPlace to cachedStamps else null) {
        coroutineScope {
            val place = async { repository.place(placeId) }
            val stamps = async { repository.stampBoard() }
            place.await() to stamps.await()
        }
    }
    RemoteContent(data, onBack = onBack) { (place, stamps) -> content(place, stamps) }
}

/**
 * 03-A · 장소 갤러리. 첫 페이지를 받아 보여주고, 끝까지 보면 다음 페이지를 이어 받는다.
 * '방명록 쓰기'는 현재 위치로 방문 확인을 통과해야 [onVisitAllowed].
 */
@Composable
internal fun GalleryRoute(
    repository: WattagamRepository,
    placeId: String,
    onBack: () -> Unit,
    onPlaceInfoClick: () -> Unit,
    onVisitAllowed: () -> Unit,
    onGuestbookClick: (String) -> Unit,
) {
    val first = rememberRemote(placeId) { repository.place(placeId) to repository.gallery(placeId, page = 0) }
    RemoteContent(first, onBack = onBack) { (place, firstPage) ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val onAccountProblem = LocalAccountProblemHandler.current
        val entries = remember(firstPage) { mutableStateListOf<GuestbookEntry>().apply { addAll(firstPage.entries) } }
        var page by remember(firstPage) { mutableIntStateOf(0) }
        var hasMore by remember(firstPage) { mutableStateOf(firstPage.hasNext) }
        var loadingMore by remember { mutableStateOf(false) }
        var moreError by remember { mutableStateOf<String?>(null) }
        val visitCheck = rememberVisitCheck(judge = { repository.checkVisit(placeId, it) }, onAllowed = { onVisitAllowed() })

        GalleryScreen(
            uiState = GalleryUiState(place.name, entries.map { it.card(meta = it.author) }, hasMore, loadingMore),
            onBack = onBack,
            onPlaceInfoClick = onPlaceInfoClick,
            onWriteClick = visitCheck.start,
            onGuestbookClick = onGuestbookClick,
            locationStatus = visitCheck.status,
            onLoadMore = {
                if (!loadingMore) {
                    loadingMore = true
                    scope.launch {
                        try {
                            val next = repository.gallery(placeId, page + 1)
                            page += 1
                            // 그사이 새 기록이 올라오면 페이지가 밀려 같은 기록이 다시 올 수 있다
                            val known = entries.map { it.id }.toSet()
                            entries.addAll(next.entries.filter { it.id !in known })
                            hasMore = next.hasNext
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: AccountProblem) {
                            onAccountProblem(e)
                        } catch (e: Exception) {
                            moreError = e.userMessage(context)
                        } finally {
                            loadingMore = false
                        }
                    }
                }
            },
        )
        moreError?.let { message ->
            AlertDialog(
                onDismissRequest = { moreError = null },
                title = { Text(stringResource(R.string.error_load_title)) },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { moreError = null }) { Text(stringResource(R.string.write_ok)) } },
            )
        }
    }
}

/**
 * 09-A-1 · 기록 상세. 서버에 연결돼 있고 내 기록이면 메시지 수정·삭제를 할 수 있다.
 * 삭제에 성공하면 [onDeleted] (보통 뒤로 가기. 갤러리·마이페이지는 돌아갈 때 다시 불러온다).
 */
@Composable
internal fun GuestbookDetailRoute(
    repository: WattagamRepository,
    entryId: String,
    onBack: () -> Unit,
    onPlaceGalleryClick: (placeId: String) -> Unit,
    onDeleted: () -> Unit,
) {
    val loaded = rememberRemote(entryId) { repository.guestbook(entryId) }
    RemoteContent(loaded, onBack = onBack) { initial ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val onAccountProblem = LocalAccountProblemHandler.current
        var entry by remember(initial) { mutableStateOf(initial) }
        var working by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf<String?>(null) }
        val canManage = entry.mine && repository.connectedToServer

        fun perform(action: suspend () -> Unit) {
            if (working) return
            working = true
            scope.launch {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: AccountProblem) {
                    onAccountProblem(e)
                } catch (e: Exception) {
                    failure = e.userMessage(context)
                } finally {
                    working = false
                }
            }
        }

        GuestbookDetailScreen(
            uiState = GuestbookDetailUiState(
                entry.author, entry.createdAt, entry.placeName, repository.courseName, entry.message, entry.mine, entry.imageUrl, entry.edited,
            ),
            onBack = onBack,
            onPlaceGalleryClick = { onPlaceGalleryClick(entry.placeId) },
            // 신고는 서버 명세에서 빠진 기능: 샘플 화면에서만 확인 창을 보여준다
            onReport = if (repository.connectedToServer) null else { {} },
            onEditMessage = if (canManage) { message -> perform { entry = repository.updateGuestbook(entry.id, message) } } else null,
            onDelete = if (canManage) { { perform { repository.deleteGuestbook(entry.id); onDeleted() } } } else null,
            working = working,
        )
        failure?.let { message ->
            AlertDialog(
                onDismissRequest = { failure = null },
                title = { Text(stringResource(R.string.gallery_action_failed)) },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { failure = null }) { Text(stringResource(R.string.write_ok)) } },
            )
        }
    }
}

/** 방명록 카드. 제목이 없는 서버 기록은 메시지 첫 줄, 메시지도 없으면 '사진만 남긴 기록'. [meta] 뒤에 작성일을 붙인다 */
@Composable
internal fun GuestbookEntry.card(meta: String): GuestbookCardUiState {
    val cardTitle = this.title ?: message.lineSequence().firstOrNull { it.isNotBlank() } ?: stringResource(R.string.guestbook_photo_only)
    val date = createdAt.substringBeforeLast(' ')
    val editedMark = if (this.edited) " · ${stringResource(R.string.gallery_edited)}" else ""
    return GuestbookCardUiState(id, cardTitle, "$meta · $date$editedMark", imageUrl)
}
