package com.wattagam.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.wattagam.app.ui.components.WattagamButton
import com.wattagam.app.ui.components.WattagamTextField
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

/**
 * 닉네임 형식 (피그마 01-A): 한글·영문·숫자 2~10자.
 * 닉네임 계정 계약은 아직 UI 제안 단계라 중복 확인은 API 단계에서 추가한다.
 */
object NicknameRule {
    const val MAX_LENGTH = 10
    private val pattern = Regex("^[가-힣a-zA-Z0-9]{2,10}$")
    fun isValid(nickname: String): Boolean = pattern.matches(nickname)
}

/**
 * 01-A · 닉네임 설정 (최초 가입) / 설정에서 닉네임 수정.
 * 형식이 맞지 않으면 완료 버튼이 비활성이라 [onSubmit] 이 호출되지 않는다.
 */
@Composable
fun NicknameScreen(
    isEdit: Boolean,
    initialNickname: String,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    submitting: Boolean = false,
    /** 서버가 거절한 이유. 보낸 닉네임을 고치기 전까지만 보여준다 */
    serverError: String? = null,
) {
    var nickname by rememberSaveable { mutableStateOf(initialNickname) }
    var submitted by rememberSaveable { mutableStateOf<String?>(null) }
    val valid = NicknameRule.isValid(nickname)
    val rejected = serverError != null && submitted == nickname
    val showError = (nickname.isNotEmpty() && !valid) || rejected

    Scaffold(
        modifier,
        topBar = {
            AppTopBar(
                stringResource(if (isEdit) R.string.auth_nickname_edit_title else R.string.auth_nickname_title),
                stringResource(R.string.back), onBack,
                Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            Column(
                Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)).padding(WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium),
            ) {
                if (!isEdit) {
                    Text(stringResource(R.string.auth_nickname_next_notice), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
                }
                WattagamButton(
                    stringResource(if (isEdit) R.string.auth_nickname_save else R.string.auth_nickname_next),
                    onClick = {
                        submitted = nickname
                        onSubmit(nickname)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = valid && !rejected,
                    loading = submitting,
                )
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Screen),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Small)) {
                Text(stringResource(R.string.auth_nickname_heading), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.auth_nickname_body), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
            }
            WattagamTextField(
                value = nickname,
                onValueChange = { nickname = it.trim().take(NicknameRule.MAX_LENGTH) },
                label = stringResource(R.string.nickname),
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (rejected) {
                    serverError.orEmpty()
                } else if (showError) {
                    stringResource(R.string.nickname_error)
                } else {
                    stringResource(R.string.auth_nickname_counter, nickname.length, NicknameRule.MAX_LENGTH)
                },
                isError = showError,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun NicknamePreview() {
    WattagamTheme { NicknameScreen(isEdit = false, initialNickname = "하늘", onBack = {}, onSubmit = {}) }
}
