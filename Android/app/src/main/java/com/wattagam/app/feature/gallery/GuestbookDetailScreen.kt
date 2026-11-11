package com.wattagam.app.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.wattagam.app.ui.components.GuestbookPhoto
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.components.WattagamTextField
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
    val imageUrl: String? = null,
    val edited: Boolean = false,
)

/** 메시지 최대 길이 (서버 기준, 글자 수) */
private const val MESSAGE_LIMIT = 80

/**
 * 09-A-1 · 다른 사람의 공개 기록 / 내 기록 보기.
 * 내 기록이면 [onEditMessage]·[onDelete] 로 메시지 수정(사진·장소는 바꿀 수 없음)과 삭제를 한다 (서버 연결 시에만 넘긴다).
 * 메시지가 바뀌면 수정 창을 닫는다. [working] 동안은 같은 동작을 다시 보내지 않는다.
 * [onReport] 가 있으면(샘플 화면) 다른 사람의 기록에 '신고'를 보여준다. 신고는 서버 명세에서 빠진 기능이다.
 */
@Composable
fun GuestbookDetailScreen(
    uiState: GuestbookDetailUiState,
    onBack: () -> Unit,
    onPlaceGalleryClick: () -> Unit,
    onReport: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onEditMessage: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    working: Boolean = false,
) {
    var confirmReport by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // 수정 중인 메시지. null 이면 수정 창이 닫혀 있다
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.message) { editing = null }

    Scaffold(
        modifier,
        topBar = {
            AppTopBar(
                stringResource(if (uiState.mine) R.string.gallery_detail_mine_title else R.string.gallery_detail_title),
                stringResource(R.string.back), onBack,
                Modifier.statusBarsPadding(),
                actionLabel = when {
                    uiState.mine -> onEditMessage?.let { stringResource(R.string.gallery_edit) }
                    else -> onReport?.let { stringResource(R.string.gallery_report) }
                },
                onAction = { if (uiState.mine) editing = uiState.message else confirmReport = true },
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(WattagamSpacing.Screen), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
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
                Text(
                    if (uiState.edited) "${uiState.createdAt} · ${stringResource(R.string.gallery_edited)}" else uiState.createdAt,
                    style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                )
            }
            GuestbookPhoto(uiState.imageUrl, stringResource(R.string.photo_placeholder), height = 280.dp)
            if (uiState.message.isNotBlank()) {
                Text(uiState.message, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = WattagamColors.Border)
            Text(
                stringResource(R.string.gallery_detail_place, uiState.courseName, uiState.placeName),
                style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted,
            )
            if (uiState.mine && onDelete != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(), enabled = !working) {
                    Text(stringResource(R.string.gallery_delete), color = WattagamColors.Danger)
                }
            }
        }
    }

    editing?.let { draft ->
        val length = draft.codePointCount(0, draft.length)
        val tooLong = length > MESSAGE_LIMIT
        AlertDialog(
            onDismissRequest = { if (!working) editing = null },
            title = { Text(stringResource(R.string.gallery_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Small)) {
                    Text(stringResource(R.string.gallery_edit_note), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
                    WattagamTextField(
                        value = draft,
                        onValueChange = { editing = it },
                        label = stringResource(R.string.gallery_edit_label),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = stringResource(R.string.gallery_edit_counter, length, MESSAGE_LIMIT),
                        isError = tooLong,
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onEditMessage?.invoke(draft) },
                    enabled = !working && !tooLong && draft.trim() != uiState.message,
                ) { Text(stringResource(R.string.gallery_edit_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }, enabled = !working) { Text(stringResource(R.string.gallery_report_cancel)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.gallery_delete_title)) },
            text = { Text(stringResource(R.string.gallery_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) {
                    Text(stringResource(R.string.gallery_delete_confirm), color = WattagamColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.gallery_report_cancel)) } },
        )
    }

    if (confirmReport) {
        AlertDialog(
            onDismissRequest = { confirmReport = false },
            title = { Text(stringResource(R.string.gallery_report_title)) },
            text = { Text(stringResource(R.string.gallery_report_body)) },
            confirmButton = {
                TextButton(onClick = { confirmReport = false; onReport?.invoke() }) {
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
