package com.wattagam.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamTheme

enum class MainTab { Map, MyPage }

/** 하단 탐색 · BottomNavigation (지도 / 마이페이지). 선택 상태는 현재 화면에서 결정 */
@Composable
fun BottomNavigation(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(38.dp),
        color = WattagamColors.Surface,
        border = BorderStroke(1.dp, WattagamColors.Border),
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            TabItem(stringResource(R.string.nav_map), selected == MainTab.Map, Modifier.weight(1f)) { onSelect(MainTab.Map) }
            TabItem(stringResource(R.string.nav_mypage), selected == MainTab.MyPage, Modifier.weight(1f)) { onSelect(MainTab.MyPage) }
        }
    }
}

@Composable
private fun TabItem(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = WattagamSizes.TouchTarget + 20.dp)
            .semantics { selected = active; role = Role.Tab },
        shape = RoundedCornerShape(30.dp),
    ) {
        Text(
            label,
            style = if (active) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
            color = if (active) WattagamColors.Primary else WattagamColors.Muted,
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun BottomNavigationPreview() {
    WattagamTheme { BottomNavigation(MainTab.Map, {}) }
}
