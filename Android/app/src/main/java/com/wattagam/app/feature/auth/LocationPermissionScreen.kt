package com.wattagam.app.feature.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * 01-B · 위치 허용 (온보딩) — 권한 요청 연결.
 * 허용·거절과 관계없이 지도로 이동한다. 거절해도 지도는 둘러볼 수 있고, 작성할 때 다시 확인한다.
 */
@Composable
fun LocationPermissionRoute(onBack: () -> Unit, onDone: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onDone() }
    LocationPermissionScreen(
        onBack = onBack,
        onAllow = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        onLater = onDone,
    )
}

@Composable
fun LocationPermissionScreen(onBack: () -> Unit, onAllow: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier,
        containerColor = WattagamColors.Soft,
        contentColor = WattagamColors.Ink,
        topBar = { AppTopBar(stringResource(R.string.auth_permission_title), stringResource(R.string.back), onBack, Modifier.statusBarsPadding()) },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(WattagamSpacing.Screen), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Small)) {
                WattagamButton(stringResource(R.string.auth_permission_allow), onAllow, Modifier.fillMaxWidth())
                WattagamButton(stringResource(R.string.auth_permission_later), onLater, Modifier.fillMaxWidth(), kind = ButtonKind.Text)
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(WattagamSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Content),
        ) {
            Text(stringResource(R.string.auth_permission_heading), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.auth_permission_body), style = MaterialTheme.typography.bodyMedium, color = WattagamColors.Muted)
            StatusCard(
                stringResource(R.string.auth_permission_card_title),
                stringResource(R.string.auth_permission_card_body),
                Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.auth_permission_privacy), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
            Text(stringResource(R.string.auth_permission_denied_note), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun LocationPermissionPreview() {
    WattagamTheme { LocationPermissionScreen(onBack = {}, onAllow = {}, onLater = {}) }
}
