package com.wattagam.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.StatusCard
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

// 카카오 로그인 버튼 색 (카카오 디자인 가이드)
private val KakaoYellow = Color(0xFFFEE500)
private val KakaoText = Color(0xFF191919)

/** 로그인 버튼 상태. 진행 중과 실패는 동시에 일어나지 않는다 */
enum class LoginStatus { Idle, Loading, Failed }

/**
 * 01 · 카카오 로그인.
 * [sampleMode] 는 카카오 앱 키가 없어 실제 로그인 없이 샘플 흐름으로 넘어가는 상태다.
 */
@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit,
    modifier: Modifier = Modifier,
    status: LoginStatus = LoginStatus.Idle,
    sampleMode: Boolean = true,
) {
    val loading = status == LoginStatus.Loading
    Scaffold(modifier, containerColor = WattagamColors.Soft, contentColor = WattagamColors.Ink) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Screen),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
                    Text(stringResource(R.string.auth_login_title), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.auth_login_body), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                }
                // 장식 그라데이션 대신 캠퍼스 사진과 푸앙이 (피그마 01 의 구성)
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    Image(
                        painterResource(R.drawable.bc_photo_sample), stringResource(R.string.auth_login_photo),
                        Modifier.fillMaxSize().clip(WattagamShapes.Card), contentScale = ContentScale.Crop,
                    )
                    Image(
                        painterResource(R.drawable.bc_puang), stringResource(R.string.bc_puang),
                        Modifier.align(Alignment.BottomEnd).padding(end = 8.dp).size(112.dp),
                    )
                }
            }
            Column(
                Modifier.padding(start = WattagamSpacing.Screen, end = WattagamSpacing.Screen, bottom = WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                if (status == LoginStatus.Failed) {
                    StatusCard(stringResource(R.string.auth_login_failed), stringResource(R.string.auth_login_failed_body), Modifier.fillMaxWidth())
                }
                Button(
                    onClick = onKakaoLogin,
                    modifier = Modifier.fillMaxWidth().heightIn(min = WattagamSizes.Button),
                    enabled = !loading,
                    shape = WattagamShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KakaoYellow, contentColor = KakaoText,
                        disabledContainerColor = KakaoYellow, disabledContentColor = KakaoText,
                    ),
                    elevation = null,
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.height(20.dp), color = KakaoText, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.auth_login_kakao), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(
                    stringResource(if (sampleMode) R.string.sample_notice else R.string.auth_login_server_pending),
                    style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.auth_login_terms),
                    style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun LoginPreview() {
    WattagamTheme { LoginScreen(onKakaoLogin = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916, name = "실패")
@Composable
private fun LoginFailedPreview() {
    WattagamTheme { LoginScreen(onKakaoLogin = {}, status = LoginStatus.Failed, sampleMode = false) }
}
