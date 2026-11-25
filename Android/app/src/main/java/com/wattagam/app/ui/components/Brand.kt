package com.wattagam.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamTheme

/** 왔다감 로고 (물결 테두리 + 하트 핀). 옆에 앱 이름이 있으면 설명은 비워 둔다 */
@Composable
fun WattagamLogo(modifier: Modifier = Modifier, size: Dp = 32.dp, contentDescription: String? = null) {
    Image(painterResource(R.drawable.ic_wattagam_logo), contentDescription, modifier.size(size))
}

/** 로고와 앱 이름 · 로그인 화면 같은 브랜드 자리 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    logoSize: Dp = 36.dp,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WattagamLogo(size = logoSize)
        Text(stringResource(R.string.app_name), style = style)
    }
}

@Preview(showBackground = true)
@Composable
private fun BrandPreview() {
    WattagamTheme {
        Column(Modifier.padding(24.dp)) {
            BrandMark()
            WattagamLogo(size = 64.dp)
        }
    }
}
