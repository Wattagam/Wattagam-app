package com.wattagam.app.feature.mypage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.BottomNavigation
import com.wattagam.app.ui.components.ButtonKind
import com.wattagam.app.ui.components.GuestbookCardUiState
import com.wattagam.app.ui.components.GuestbookGrid
import com.wattagam.app.ui.components.MainTab
import com.wattagam.app.ui.components.StampBoard
import com.wattagam.app.ui.components.StampUiState
import com.wattagam.app.ui.components.StatusCard
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

/** 마이페이지 기록 한 칸. placeId 로 장소 필터를 건다 */
data class MyGuestbookUiState(val placeId: String, val card: GuestbookCardUiState)

data class MyPageUiState(
    val nickname: String,
    val stamps: List<StampUiState>,
    val guestbooks: List<MyGuestbookUiState>,
)

/** 09-B · 마이페이지 (StampBoard · GuestbookGrid · BottomNavigation) */
@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onSettingsClick: () -> Unit,
    onGuestbookClick: (guestbookId: String) -> Unit,
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) } // null = 전체
    val shown = uiState.guestbooks.filter { filter == null || it.placeId == filter }

    Scaffold(
        modifier,
        containerColor = WattagamColors.Soft,
        contentColor = WattagamColors.Ink,
        topBar = {
            AppTopBar(
                stringResource(R.string.mypage_title), backLabel = null, onBack = {}, modifier = Modifier.statusBarsPadding(),
                actionLabel = stringResource(R.string.mypage_settings), onAction = onSettingsClick,
            )
        },
        bottomBar = { BottomNavigation(MainTab.MyPage, onTabSelect) },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.mypage_heading, uiState.nickname), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.mypage_count, uiState.guestbooks.size),
                style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted,
            )
            StampBoard(uiState.stamps, Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceFilterChip(stringResource(R.string.mypage_filter_all), filter == null) { filter = null }
                uiState.stamps.forEach { stamp ->
                    PlaceFilterChip(stamp.name, filter == stamp.placeId) { filter = stamp.placeId }
                }
            }
            if (shown.isEmpty()) {
                StatusCard(
                    stringResource(R.string.mypage_empty_title), stringResource(R.string.mypage_empty_body), Modifier.fillMaxWidth(),
                    action = { WattagamButton(stringResource(R.string.mypage_go_map), { onTabSelect(MainTab.Map) }, kind = ButtonKind.Text) },
                )
            } else {
                GuestbookGrid(shown.map { it.card }, onGuestbookClick)
            }
        }
    }
}

/** 장소 필터 · PlaceFilterChip — 마이페이지에서만 쓰여 기능 안에 둔다 */
@Composable
private fun PlaceFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = WattagamShapes.Pill,
        color = if (selected) WattagamColors.Ink else WattagamColors.Surface,
        modifier = Modifier.heightIn(min = WattagamSizes.TouchTarget).semantics { this.selected = selected },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) WattagamColors.Surface else WattagamColors.Muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun MyPagePreview() {
    WattagamTheme {
        MyPageScreen(
            MyPageUiState(
                "하늘",
                listOf(
                    StampUiState("gate", "정문", true), StampUiState("library", "도서관", true),
                    StampUiState("pond", "청룡탕", false), StampUiState("310", "310관", false),
                ),
                listOf(
                    MyGuestbookUiState("library", GuestbookCardUiState("g1", "햇살까지 기억할 오후", "도서관 · 2026. 10. 27.")),
                    MyGuestbookUiState("gate", GuestbookCardUiState("g7", "산책의 시작은 가볍게", "정문 · 2026. 10. 27.")),
                ),
            ),
            onSettingsClick = {}, onGuestbookClick = {}, onTabSelect = {},
        )
    }
}
