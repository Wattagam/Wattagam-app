package com.wattagam.app.feature.mypage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.wattagam.app.R
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.ButtonKind
import com.wattagam.app.ui.components.StatusCard
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

/**
 * 09-B-3 · 계정 설정 (닉네임 수정 / 로그아웃 / 회원 탈퇴 확인).
 * 로그인 API 연결 전이라 로그아웃·탈퇴는 이벤트만 전달한다.
 */
@Composable
fun SettingsScreen(
    nickname: String,
    guestbookCount: Int,
    onBack: () -> Unit,
    onEditNickname: () -> Unit,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier,
        topBar = { AppTopBar(stringResource(R.string.mypage_settings), stringResource(R.string.back), onBack) },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Content),
        ) {
            Column {
                Text(nickname, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.mypage_settings_account), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
            }
            WattagamButton(stringResource(R.string.mypage_settings_edit_nickname), onEditNickname, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
            StatusCard(
                stringResource(R.string.mypage_settings_privacy_title),
                stringResource(R.string.mypage_settings_privacy_body),
                Modifier.fillMaxWidth(), highlighted = true,
            )
            WattagamButton(stringResource(R.string.mypage_settings_logout), onLogout, Modifier.fillMaxWidth(), kind = ButtonKind.Text)
            TextButton(onClick = { confirmWithdraw = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.mypage_settings_withdraw), color = WattagamColors.Danger)
            }
        }
    }

    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text(stringResource(R.string.mypage_withdraw_title)) },
            text = { Text(stringResource(R.string.mypage_withdraw_body, nickname, guestbookCount)) },
            confirmButton = {
                TextButton(onClick = { confirmWithdraw = false; onWithdraw() }) {
                    Text(stringResource(R.string.mypage_withdraw_confirm), color = WattagamColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWithdraw = false }) { Text(stringResource(R.string.mypage_withdraw_cancel)) }
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun SettingsPreview() {
    WattagamTheme {
        SettingsScreen("하늘", 2, onBack = {}, onEditNickname = {}, onLogout = {}, onWithdraw = {})
    }
}
