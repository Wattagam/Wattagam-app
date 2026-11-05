package com.wattagam.app.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.ButtonKind
import com.wattagam.app.ui.components.PhotoPlaceholder
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

data class GuestbookDetailUiState(
    val author: String,
    val createdAt: String,
    val placeName: String,
    val courseName: String,
    val message: String,
    val mine: Boolean,
)

/**
 * 09-A-1 · 다른 사람의 공개 기록 / 내 기록 보기.
 * 수정·삭제는 이번 MVP 제외. 다른 사람의 기록에만 '신고'를 보여준다 (신고 API 는 연결 전).
 */
@Composable
fun GuestbookDetailScreen(
    uiState: GuestbookDetailUiState,
    onBack: () -> Unit,
    onPlaceGalleryClick: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmReport by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier,
        topBar = {
            AppTopBar(
                stringResource(if (uiState.mine) R.string.gallery_detail_mine_title else R.string.gallery_detail_title),
                stringResource(R.string.back), onBack,
                actionLabel = if (uiState.mine) null else stringResource(R.string.gallery_report),
                onAction = { confirmReport = true },
            )
        },
        bottomBar = {
            Column(Modifier.padding(WattagamSpacing.Screen), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
                if (!uiState.mine) {
                    Text(
                        stringResource(R.string.gallery_detail_notice, uiState.placeName, uiState.author),
                        style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                    )
                }
                WattagamButton(
                    stringResource(R.string.gallery_detail_all, uiState.placeName),
                    onPlaceGalleryClick, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary,
                )
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column {
                Text(uiState.author, style = MaterialTheme.typography.labelMedium)
                Text(uiState.createdAt, style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
            }
            PhotoPlaceholder(height = 280.dp)
            if (uiState.message.isNotBlank()) {
                Text(uiState.message, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = WattagamColors.Border)
            Text(
                stringResource(R.string.gallery_detail_place, uiState.courseName, uiState.placeName),
                style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted,
            )
        }
    }

    if (confirmReport) {
        AlertDialog(
            onDismissRequest = { confirmReport = false },
            title = { Text(stringResource(R.string.gallery_report_title)) },
            text = { Text(stringResource(R.string.gallery_report_body)) },
            confirmButton = {
                TextButton(onClick = { confirmReport = false; onReport() }) {
                    Text(stringResource(R.string.gallery_report), color = WattagamColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReport = false }) { Text(stringResource(R.string.gallery_report_cancel)) } },
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun GuestbookDetailPreview() {
    WattagamTheme {
        GuestbookDetailScreen(
            GuestbookDetailUiState(
                "유진", "2026. 10. 26. 16:10", "도서관", "푸른캠퍼스 산책",
                "창가에 앉아 책을 읽다 보니, 어느새 해가 기울었다. 다음 산책에도 꼭 들러야지.", mine = false,
            ),
            onBack = {}, onPlaceGalleryClick = {}, onReport = {},
        )
    }
}
