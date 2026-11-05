package com.wattagam.app.feature.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.ButtonKind
import com.wattagam.app.ui.components.PhotoPlaceholder
import com.wattagam.app.ui.components.StatusCard
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamColors
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
) {
    Scaffold(
        modifier,
        topBar = { AppTopBar(stringResource(R.string.place_title), stringResource(R.string.back), onBack) },
        bottomBar = {
            Column(Modifier.padding(WattagamSpacing.Screen), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Small)) {
                Text(
                    stringResource(R.string.stamp_rule),
                    style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                WattagamButton(stringResource(R.string.place_write), onWriteClick, Modifier.fillMaxWidth())
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())) {
            PhotoPlaceholder(Modifier.padding(horizontal = WattagamSpacing.Screen), height = 200.dp)
            Column(
                Modifier.padding(WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Content),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.place_order, uiState.courseName, uiState.order),
                            style = MaterialTheme.typography.labelSmall, color = WattagamColors.Primary,
                        )
                        Text(
                            stringResource(if (uiState.stamped) R.string.earned else R.string.unearned),
                            style = MaterialTheme.typography.labelSmall, color = WattagamColors.Muted,
                        )
                    }
                    Text(uiState.name, style = MaterialTheme.typography.headlineLarge)
                    Text(uiState.description, style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                }
                Text(uiState.highlight, style = MaterialTheme.typography.labelMedium)
                StatusCard(stringResource(R.string.place_prompt_title), uiState.writingPrompt, Modifier.fillMaxWidth(), highlighted = true)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.place_directions), style = MaterialTheme.typography.labelLarge)
                    Text(uiState.directions, style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                    Text(
                        stringResource(R.string.place_checkpoint, uiState.checkpoint),
                        style = MaterialTheme.typography.labelSmall, color = WattagamColors.Primary,
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
        )
    }
}
