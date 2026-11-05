package com.wattagam.app.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wattagam.app.R
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.ButtonKind
import com.wattagam.app.ui.components.GuestbookCardUiState
import com.wattagam.app.ui.components.GuestbookGrid
import com.wattagam.app.ui.components.StatusCard
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

data class GalleryUiState(val placeName: String, val items: List<GuestbookCardUiState>)

private const val PAGE_SIZE = 4 // 피그마: "기록 더 보기 · 4 / 23"

/**
 * 03-A · 공개 갤러리 열람 / 09-A · 업로드 후 장소 갤러리 (같은 화면).
 * 샘플 단계라 전체 목록을 받아 4개씩 더 보여준다. API 단계에서 페이지 요청으로 바꾼다.
 */
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onBack: () -> Unit,
    onPlaceInfoClick: () -> Unit,
    onWriteClick: () -> Unit,
    onGuestbookClick: (guestbookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by rememberSaveable { mutableIntStateOf(PAGE_SIZE) }
    val visible = uiState.items.take(shown)

    Scaffold(
        modifier,
        topBar = { AppTopBar(stringResource(R.string.gallery_title), stringResource(R.string.back), onBack) },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium),
        ) {
            Text(uiState.placeName, style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.gallery_description), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
            WattagamButton(stringResource(R.string.gallery_place_info), onPlaceInfoClick, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
            WattagamButton(stringResource(R.string.place_write), onWriteClick, Modifier.fillMaxWidth())
            Text(stringResource(R.string.gallery_public_records), style = MaterialTheme.typography.titleLarge)
            if (visible.isEmpty()) {
                StatusCard(stringResource(R.string.empty_title), stringResource(R.string.empty_body), Modifier.fillMaxWidth())
            } else {
                GuestbookGrid(visible, onGuestbookClick)
            }
            if (shown < uiState.items.size) {
                WattagamButton(
                    stringResource(R.string.gallery_more, visible.size, uiState.items.size),
                    onClick = { shown += PAGE_SIZE },
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Text,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun GalleryPreview() {
    WattagamTheme {
        GalleryScreen(
            GalleryUiState(
                "도서관",
                (1..6).map { GuestbookCardUiState("g$it", "도서관의 오후 $it", "유진 · 2026. 10. 2$it.") },
            ),
            onBack = {}, onPlaceInfoClick = {}, onWriteClick = {}, onGuestbookClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916, name = "빈 목록")
@Composable
private fun GalleryEmptyPreview() {
    WattagamTheme {
        GalleryScreen(GalleryUiState("310관", emptyList()), onBack = {}, onPlaceInfoClick = {}, onWriteClick = {}, onGuestbookClick = {})
    }
}
