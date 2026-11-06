package com.wattagam.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.auth.KakaoLoginResult
import kotlinx.coroutines.launch

/**
 * 01 · 로그인 경로. 카카오 앱 키가 있으면 실제 카카오 로그인, 없으면 샘플 흐름으로 바로 넘어간다.
 * 들어올 때 이전 카카오 로그인이 살아 있으면 버튼을 누르지 않아도 [onLoggedIn] 을 부른다 (자동 로그인).
 * 서버 로그인 API 가 나오면 [KakaoLoginResult.Success] 의 토큰을 서버에 보내고, 그 결과로 [onLoggedIn] 을 부른다.
 */
@Composable
fun LoginRoute(onLoggedIn: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sampleMode = !KakaoLogin.isConfigured
    var status by remember { mutableStateOf(if (sampleMode) LoginStatus.Idle else LoginStatus.Loading) }
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)

    // 자동 로그인 확인 (확인하는 동안 버튼은 진행 중으로 보인다)
    LaunchedEffect(Unit) {
        if (sampleMode) return@LaunchedEffect
        if (KakaoLogin.hasSession()) currentOnLoggedIn() else status = LoginStatus.Idle
    }

    LoginScreen(
        onKakaoLogin = {
            when {
                sampleMode -> onLoggedIn()
                status != LoginStatus.Loading -> {
                    status = LoginStatus.Loading
                    scope.launch {
                        when (KakaoLogin.login(context)) {
                            is KakaoLoginResult.Success -> {
                                status = LoginStatus.Idle
                                onLoggedIn()
                            }
                            KakaoLoginResult.Cancelled -> status = LoginStatus.Idle
                            KakaoLoginResult.Failed -> status = LoginStatus.Failed
                        }
                    }
                }
            }
        },
        modifier = modifier,
        status = status,
        sampleMode = sampleMode,
    )
}
