package com.wattagam.app.feature.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.ButtonKind
import com.wattagam.app.ui.components.LocationStatus
import com.wattagam.app.ui.components.LocationUiState
import com.wattagam.app.ui.components.PlaceStamp
import com.wattagam.app.ui.components.StatusCard
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

data class PlaceDetailUiState(
    val courseName: String,
    val order: Int,
    val name: String,
    val description: String,
    val highlight: String,
    val writingPrompt: String,
    val directions: String,
    val checkpoint: String,
    val stamped: Boolean,
    val publicCount: Int,
)

/**
 * 03 · 장소 설명 (E · 다른 장소의 설명 화면도 이 화면에 데이터만 바꿔 표시).
 * 이 화면을 여는 것은 열람일 뿐 방문 인정이 아니다. 작성은 [onWriteClick] 으로 촬영 흐름에 넘긴다.
 */
@Composable
fun PlaceDetailScreen(
    uiState: PlaceDetailUiState,
    onBack: () -> Unit,
    onGalleryClick: () -> Unit,
    onWriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    locationStatus: LocationUiState? = null,
) {
    Scaffold(
        modifier,
        topBar = { AppTopBar(stringResource(R.string.place_title), stringResource(R.string.back), onBack, Modifier.statusBarsPadding()) },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(WattagamSpacing.Screen), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Small)) {
                Text(
                    stringResource(R.string.stamp_rule),
                    style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                // 작성 시작 위치 확인 결과 (확인 중 / 범위 밖 / 권한 없음 / 확인 실패 / 작성 가능)
                if (locationStatus != null) LocationStatus(locationStatus, Modifier.fillMaxWidth())
                WattagamButton(
                    stringResource(R.string.place_write), onWriteClick, Modifier.fillMaxWidth(),
                    loading = locationStatus == LocationUiState.Checking,
                )
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())) {
            // 장소 사진이 서버에서 오기 전까지는 '사진' 자리 대신 이 장소에서 받을 수 있는 도장을 보여준다
            Surface(
                Modifier.padding(horizontal = WattagamSpacing.Screen).fillMaxWidth().height(200.dp),
                shape = WattagamShapes.Card, color = WattagamColors.Soft,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlaceStamp(uiState.name, uiState.stamped, size = 112.dp)
                    Text(
                        stringResource(if (uiState.stamped) R.string.place_stamp_caption_earned else R.string.place_stamp_caption_empty),
                        style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                    )
                }
            }
            Column(
                Modifier.padding(WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Content),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.place_order, uiState.courseName, uiState.order),
                        style = MaterialTheme.typography.labelSmall, color = WattagamColors.Muted,
                    )
                    Text(uiState.name, style = MaterialTheme.typography.headlineLarge)
                    Text(uiState.description, style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                }
                Text(uiState.highlight, style = MaterialTheme.typography.labelMedium)
                StatusCard(stringResource(R.string.place_prompt_title), uiState.writingPrompt, Modifier.fillMaxWidth())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.place_directions), style = MaterialTheme.typography.labelLarge)
                    Text(uiState.directions, style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                    Text(
                        stringResource(R.string.place_checkpoint, uiState.checkpoint),
                        style = MaterialTheme.typography.labelSmall, color = WattagamColors.Ink,
                    )
                }
                WattagamButton(
                    stringResource(R.string.place_open_gallery, uiState.publicCount),
                    onGalleryClick, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun PlaceDetailPreview() {
    WattagamTheme {
        PlaceDetailScreen(
            PlaceDetailUiState(
                "푸른캠퍼스 산책", 2, "도서관",
                "커다란 창 너머로 오후의 햇살이 길게 들어오는 곳이에요.",
                "계단에서 바라보는 창가의 빛과 그림자",
                "오늘의 풍경에 손그림과 짧은 메시지를 더해보세요.",
                "정문에서 본관 오른쪽으로 가면 넓은 계단이 보여요.",
                "도서관 앞 계단", stamped = false, publicCount = 6,
            ),
            onBack = {}, onGalleryClick = {}, onWriteClick = {},
            locationStatus = LocationUiState.Outside,
        )
    }
}
