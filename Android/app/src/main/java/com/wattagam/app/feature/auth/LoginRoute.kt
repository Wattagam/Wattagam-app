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
import com.wattagam.app.data.Account
import com.wattagam.app.data.WattagamRepository
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.auth.KakaoLoginResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 01 · 로그인 경로. 카카오 앱 키가 있으면 실제 카카오 로그인, 없으면 샘플 흐름으로 바로 넘어간다.
 * 카카오 로그인 결과는 [repository] 가 서버 토큰으로 바꾼다 (서버 주소가 없으면 샘플 계정).
 * 들어올 때 이전 로그인이 살아 있으면 버튼을 누르지 않아도 [onLoggedIn] 을 부른다 (자동 로그인).
 */
@Composable
fun LoginRoute(repository: WattagamRepository, onLoggedIn: (Account) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sampleMode = !KakaoLogin.isConfigured
    var status by remember { mutableStateOf(if (sampleMode) LoginStatus.Idle else LoginStatus.Loading) }
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)

    // 자동 로그인 확인 (확인하는 동안 버튼은 진행 중으로 보인다)
    LaunchedEffect(Unit) {
        if (sampleMode) return@LaunchedEffect
        status = try {
            val account = repository.restoreSession()
            if (account != null) currentOnLoggedIn(account)
            LoginStatus.Idle
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            LoginStatus.Failed
        }
    }

    LoginScreen(
        onKakaoLogin = {
            if (status != LoginStatus.Loading) {
                status = LoginStatus.Loading
                scope.launch {
                    val kakaoToken = if (sampleMode) {
                        null
                    } else {
                        when (val result = KakaoLogin.login(context)) {
                            is KakaoLoginResult.Success -> result.kakaoAccessToken
                            KakaoLoginResult.Cancelled -> {
                                status = LoginStatus.Idle
                                return@launch
                            }
                            KakaoLoginResult.Failed -> {
                                status = LoginStatus.Failed
                                return@launch
                            }
                        }
                    }
                    status = try {
                        onLoggedIn(repository.signIn(kakaoToken))
                        LoginStatus.Idle
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 서버가 카카오 토큰을 받지 못했거나 연결 실패: 버튼으로 다시 시도
                        LoginStatus.Failed
                    }
                }
            }
        },
        modifier = modifier,
        status = status,
        sampleMode = sampleMode,
        serverConnected = repository.connectedToServer,
    )
}
