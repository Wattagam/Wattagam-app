package com.wattagam.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

// 카카오 로그인 버튼 색 (카카오 디자인 가이드)
private val KakaoYellow = Color(0xFFFEE500)
private val KakaoText = Color(0xFF191919)
private val HeroGradient = Brush.horizontalGradient(listOf(Color(0xFF1848D5), Color(0xFF3B7CFC)))

/**
 * 01 · 카카오 로그인.
 * 카카오 SDK 연결 전이라 버튼은 [onKakaoLogin] 이벤트만 전달한다 (실제 로그인 아님).
 */
@Composable
fun LoginScreen(onKakaoLogin: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier, containerColor = WattagamColors.Soft) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Screen),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
                    Text(stringResource(R.string.auth_login_title), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.auth_login_body), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
                }
                Box(
                    Modifier.fillMaxWidth().height(240.dp).background(HeroGradient, WattagamShapes.Card).padding(20.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(stringResource(R.string.auth_login_hero), style = MaterialTheme.typography.labelMedium, color = WattagamColors.Surface)
                }
            }
            Column(
                Modifier.padding(start = WattagamSpacing.Screen, end = WattagamSpacing.Screen, bottom = WattagamSpacing.Screen),
                verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Button(
                    onClick = onKakaoLogin,
                    modifier = Modifier.fillMaxWidth().heightIn(min = WattagamSizes.Button),
                    shape = WattagamShapes.Field,
                    colors = ButtonDefaults.buttonColors(containerColor = KakaoYellow, contentColor = KakaoText),
                    elevation = null,
                ) {
                    Text(stringResource(R.string.auth_login_kakao), style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    stringResource(R.string.sample_notice),
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
