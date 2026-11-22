package com.wattagam.app.feature.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.components.PlaceStamp
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

/**
 * 장소 · PlaceListItem.
 * 누르면 장소 설명만 연다 — 방문 인정과 분리 (도장은 서버 저장 결과로만 결정).
 */
@Composable
fun PlaceListItem(
    order: Int,
    name: String,
    hint: String,
    stamped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = WattagamSizes.TouchTarget)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium),
    ) {
        Surface(color = WattagamColors.Soft, shape = WattagamShapes.Small, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(order.toString(), style = MaterialTheme.typography.labelSmall, color = WattagamColors.Ink)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            // 서버 장소 목록에는 확인 지점 이름이 없어 비어 있을 수 있다
            if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
        }
        // 받은 도장은 작은 도장 자국으로, 색만으로 구분하지 않도록 글자도 함께
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (stamped) PlaceStamp(name, earned = true, size = 24.dp, showName = false)
            Text(
                stringResource(if (stamped) R.string.earned else R.string.unearned),
                style = MaterialTheme.typography.bodySmall,
                color = if (stamped) WattagamColors.StampInk else WattagamColors.Muted,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun PlaceListItemPreview() {
    WattagamTheme { PlaceListItem(2, "도서관", "장소 설명과 공개 기록 보기", stamped = false, onClick = {}) }
}
