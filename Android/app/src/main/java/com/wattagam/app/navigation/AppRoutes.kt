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

/** 방명록 카드. 제목이 없는 서버 기록은 메시지 첫 줄, 메시지도 없으면 '사진만 남긴 기록'. [meta] 뒤에 작성일을 붙인다 */
@Composable
internal fun GuestbookEntry.card(meta: String): GuestbookCardUiState {
    val cardTitle = this.title ?: message.lineSequence().firstOrNull { it.isNotBlank() } ?: stringResource(R.string.guestbook_photo_only)
    return GuestbookCardUiState(id, cardTitle, "$meta · ${createdAt.substringBeforeLast(' ')}", imageUrl)
}
