package com.wattagam.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamTheme

enum class MainTab { Map, MyPage }

/**
 * 하단 탐색 · BottomNavigation (지도 / 마이페이지). 선택 상태는 현재 화면에서 결정.
 * 알약 모양으로 떠 있는 막대: 얇은 테두리와 옅은 그림자, 선택한 탭은 아이콘 뒤 연한 파란 알약.
 */
@Composable
fun BottomNavigation(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, WattagamShapes.Pill, ambientColor = WattagamColors.Primary.copy(alpha = 0.10f), spotColor = WattagamColors.Ink.copy(alpha = 0.12f)),
        shape = WattagamShapes.Pill,
        color = WattagamColors.Surface,
        border = BorderStroke(1.dp, WattagamColors.Border),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            TabItem(stringResource(R.string.nav_map), R.drawable.ic_nav_map, selected == MainTab.Map, Modifier.weight(1f)) { onSelect(MainTab.Map) }
            TabItem(stringResource(R.string.nav_mypage), R.drawable.ic_nav_mypage, selected == MainTab.MyPage, Modifier.weight(1f)) { onSelect(MainTab.MyPage) }
        }
    }
}

@Composable
private fun TabItem(label: String, @DrawableRes icon: Int, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val indicator by animateColorAsState(if (active) WattagamColors.PrimarySoft else Color.Transparent, label = "tabIndicator")
    val ink = if (active) WattagamColors.Primary else WattagamColors.Muted
    Column(
        modifier
            .heightIn(min = WattagamSizes.TouchTarget + 16.dp)
            .clip(WattagamShapes.Field)
            .selectable(selected = active, onClick = onClick, role = Role.Tab)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        // 아이콘 설명은 아래 글자로 충분하다
        Box(
            Modifier.size(width = 58.dp, height = 30.dp).clip(WattagamShapes.Pill).background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = ink)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

@Preview(showBackground = true, widthDp = 412)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun BottomNavigationPreview() {
    WattagamTheme { BottomNavigation(MainTab.Map, {}) }
}
