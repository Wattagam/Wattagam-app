package com.wattagam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

/**
 * 장소 도장 · 왔다감만의 표시. 받은 도장은 인주색 이중 테두리 안에 장소 이름이 찍힌 자국으로,
 * 아직 받지 않은 도장은 점선으로 비워 둔 자리로 그린다. 도장판·장소 목록·장소 설명·도장 지급에만 쓴다.
 * [showName] 이 false 면 작은 표시용으로 이름 없이 테두리만 그린다.
 */
@Composable
fun PlaceStamp(name: String, earned: Boolean, modifier: Modifier = Modifier, size: Dp = 64.dp, showName: Boolean = true) {
    val description = stringResource(if (earned) R.string.stamp_earned_description else R.string.stamp_empty_description, name)
    val ink = if (earned) WattagamColors.StampInk else WattagamColors.Border
    // 이름 크기는 도장 크기와 글자 수를 따른다 (글자 크기 설정이 커져도, 이름이 길어도 안쪽 테두리 안에 들어가게)
    val nameSize = with(LocalDensity.current) { (size * minOf(0.19f, 0.6f / name.length.coerceAtLeast(1))).toSp() }
    Box(
        modifier
            .size(size)
            .rotate(if (earned) -8f else 0f)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val outer = this.size.minDimension / 2
            if (earned) {
                drawCircle(ink, radius = outer * 0.94f, style = Stroke(outer * 0.09f))
                drawCircle(ink, radius = outer * 0.76f, style = Stroke(outer * 0.04f))
            } else {
                drawCircle(
                    ink, radius = outer * 0.92f,
                    style = Stroke(outer * 0.05f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(outer * 0.16f, outer * 0.12f))),
                )
            }
        }
        if (showName) {
            Text(
                name,
                color = if (earned) WattagamColors.StampInk else WattagamColors.Muted,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = nameSize, lineHeight = nameSize, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

data class StampUiState(val placeId: String, val name: String, val earned: Boolean)

/** 도장판 · StampBoard — 네 장소의 도장을 한 줄로 */
@Composable
fun StampBoard(stamps: List<StampUiState>, modifier: Modifier = Modifier) {
    Surface(modifier, color = WattagamColors.Surface, shape = WattagamShapes.Card) {
        Column(Modifier.padding(WattagamSpacing.Content), verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium)) {
            Text(stringResource(R.string.stamp_count, stamps.count { it.earned }, stamps.size), style = MaterialTheme.typography.labelMedium)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // 좁은 화면에서도 네 개가 한 줄에 들어가게
                val stampSize = (maxWidth / stamps.size.coerceAtLeast(1) - 8.dp).coerceIn(40.dp, 64.dp)
                Row(Modifier.fillMaxWidth()) {
                    stamps.forEach {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { PlaceStamp(it.name, it.earned, size = stampSize) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun StampBoardPreview() {
    WattagamTheme {
        StampBoard(
            listOf(
                StampUiState("gate", "정문", true), StampUiState("library", "도서관", true),
                StampUiState("pond", "청룡탕", false), StampUiState("310", "310관", false),
            ),
            Modifier.padding(16.dp),
        )
    }
}
