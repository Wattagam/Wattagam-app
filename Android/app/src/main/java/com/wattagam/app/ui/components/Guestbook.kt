package com.wattagam.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

/** 방명록 카드 한 칸 (사진·제목·작성자·작성일). 서버 모델과 분리된 표시용 값 */
data class GuestbookCardUiState(
    val id: String,
    val title: String,
    /** '하늘 · 2026. 10. 27.' 처럼 작성자·작성일 */
    val meta: String,
)

/**
 * 사진 자리. 이미지 로딩 라이브러리는 이미지 연결 단계에서 추가한다.
 * 그 전까지는 사진 영역만 표시한다.
 */
@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier, height: Dp = 120.dp) {
    // 흰 화면과 연한 회색 화면 어디에서도 자리가 보이게 테두리 색을 옅게 깐다
    Surface(modifier.fillMaxWidth().height(height), shape = WattagamShapes.Card, color = WattagamColors.Border.copy(alpha = .5f)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.bc_image), stringResource(R.string.photo_placeholder), Modifier.size(24.dp), tint = WattagamColors.Muted.copy(alpha = .6f))
        }
    }
}

/** 방명록 · GuestbookGrid — 공개 기록 2열 */
@Composable
fun GuestbookGrid(items: List<GuestbookCardUiState>, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(WattagamSpacing.Content)) {
                row.forEach { GuestbookCard(it, Modifier.weight(1f)) { onClick(it.id) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GuestbookCard(item: GuestbookCardUiState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(WattagamShapes.Card).clickable(onClick = onClick).padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Small),
    ) {
        PhotoPlaceholder()
        Text(item.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.meta, style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted, maxLines = 1)
    }
}

@Preview(showBackground = true, widthDp = 412)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun GuestbookGridPreview() {
    WattagamTheme {
        GuestbookGrid(
            listOf(
                GuestbookCardUiState("1", "햇살까지 기억할 오후", "하늘 · 2026. 10. 27."),
                GuestbookCardUiState("2", "창가에서 보낸 시간", "유진 · 2026. 10. 26."),
                GuestbookCardUiState("3", "우리의 단골 자리", "서연 · 2026. 10. 25."),
            ),
            onClick = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}
