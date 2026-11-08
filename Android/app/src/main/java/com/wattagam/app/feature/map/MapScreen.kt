package com.wattagam.app.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wattagam.app.R
import com.wattagam.app.feature.map.components.PlaceListItem
import com.wattagam.app.ui.components.AppTopBar
import com.wattagam.app.ui.components.BottomNavigation
import com.wattagam.app.ui.components.MainTab
import com.wattagam.app.ui.theme.WattagamColors
import com.wattagam.app.ui.theme.WattagamShapes
import com.wattagam.app.ui.theme.WattagamSizes
import com.wattagam.app.ui.theme.WattagamSpacing
import com.wattagam.app.ui.theme.WattagamTheme

data class MapPlaceUiState(
    val id: String,
    val order: Int,
    val name: String,
    val hint: String,
    val stamped: Boolean,
    val latitude: Double,
    val longitude: Double,
)

/** 지도 위 한 점 (위도·경도) */
data class MapPoint(val latitude: Double, val longitude: Double)

data class MapUiState(val courseName: String, val places: List<MapPlaceUiState>) {
    val stampedCount: Int get() = places.count { it.stamped }
}

/**
 * 02 · 지도에서 장소 선택.
 * [useKakaoMap] 이면 카카오맵을 쓰고, 아니면(앱 키 없음·지원하지 않는 기기·지도 오류) 피그마 안내 지도 그림을 Canvas 로 그린다.
 */
@Composable
fun MapScreen(
    uiState: MapUiState,
    onPlaceClick: (placeId: String) -> Unit,
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    useKakaoMap: Boolean = false,
    myLocation: MapPoint? = null,
) {
    var mapFailed by remember { mutableStateOf(false) }
    Scaffold(
        modifier,
        containerColor = WattagamColors.Soft,
        contentColor = WattagamColors.Ink,
        topBar = { AppTopBar(uiState.courseName, backLabel = null, onBack = {}, modifier = Modifier.statusBarsPadding()) },
        bottomBar = { BottomNavigation(MainTab.Map, onTabSelect) },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = WattagamSpacing.Screen, vertical = WattagamSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(WattagamSpacing.Medium),
        ) {
            Text(
                stringResource(R.string.map_summary, uiState.places.size, uiState.stampedCount, uiState.places.size),
                style = MaterialTheme.typography.labelMedium, color = WattagamColors.Muted,
            )
            if (useKakaoMap && !mapFailed) {
                KakaoCampusMap(
                    uiState.places, onPlaceClick, onError = { mapFailed = true },
                    Modifier.fillMaxWidth().aspectRatio(MAP_W / MAP_H),
                    myLocation = myLocation,
                )
            } else {
                CampusMap(uiState.places, onPlaceClick)
            }
            Surface(color = WattagamColors.Surface, shape = WattagamShapes.Card) {
                Column(Modifier.padding(horizontal = WattagamSpacing.Medium, vertical = WattagamSpacing.Small)) {
                    uiState.places.forEach { place ->
                        PlaceListItem(place.order, place.name, place.hint, place.stamped, onClick = { onPlaceClick(place.id) })
                    }
                }
            }
            Text(stringResource(R.string.map_location_note), style = MaterialTheme.typography.bodySmall, color = WattagamColors.Muted)
        }
    }
}

// 피그마 '캠퍼스 안내 지도' 기준 좌표 (364 × 370). 핀 크기 48 = 최소 터치 영역
private const val MAP_W = 364f
private const val MAP_H = 370f
private val pinPositions = mapOf(1 to Offset(71f, 270f), 2 to Offset(156f, 106f), 3 to Offset(267f, 70f), 4 to Offset(36f, 174f))

@Composable
private fun CampusMap(places: List<MapPlaceUiState>, onPlaceClick: (String) -> Unit) {
    Surface(
        color = Color(0xFFF0F4F0),
        shape = WattagamShapes.Card,
        modifier = Modifier.fillMaxWidth().aspectRatio(MAP_W / MAP_H),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val scale = maxWidth / MAP_W
            Canvas(Modifier.fillMaxSize()) {
                val s = size.width / MAP_W
                fun o(x: Float, y: Float) = Offset(x * s, y * s)
                val road = 34f * s
                drawLine(Color.White, o(70f, -10f), o(120f, 380f), road, cap = StrokeCap.Square)
                drawLine(Color.White, o(-10f, 140f), o(380f, 70f), road, cap = StrokeCap.Square)
                drawLine(Color.White, o(150f, 170f), o(380f, 330f), road, cap = StrokeCap.Square)
                drawRoundRect(Color(0xFFD8E0DF), o(133f, 29f), Size(89f * s, 45f * s), CornerRadius(8f * s))
                drawRoundRect(Color(0xFFCCD9E9), o(159f, 143f), Size(85f * s, 55f * s), CornerRadius(8f * s))
                drawOval(Color(0xFFBFDCEB), o(266f, 74f), Size(116f * s, 134f * s))
            }
            places.forEach { place ->
                val pos = pinPositions[place.order] ?: return@forEach
                val label = stringResource(R.string.map_pin_description, place.name)
                Surface(
                    onClick = { onPlaceClick(place.id) },
                    color = WattagamColors.Primary,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(3.dp, WattagamColors.Surface),
                    modifier = Modifier
                        .offset(scale * pos.x, scale * pos.y)
                        .size(WattagamSizes.TouchTarget)
                        .semantics { contentDescription = label },
                ) {
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(place.order.toString(), style = MaterialTheme.typography.labelMedium, color = WattagamColors.Surface)
                    }
                }
                Text(
                    place.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = WattagamColors.Ink,
                    modifier = Modifier.offset(scale * pos.x, scale * (pos.y + 52f)),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 916)
@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable
private fun MapPreview() {
    WattagamTheme {
        MapScreen(
            MapUiState(
                "푸른캠퍼스 산책",
                listOf("정문", "도서관", "청룡탕", "310관").mapIndexed { i, name ->
                    MapPlaceUiState("p$i", i + 1, name, "장소 설명과 공개 기록 보기", stamped = i < 2, latitude = 0.0, longitude = 0.0)
                },
            ),
            onPlaceClick = {},
            onTabSelect = {},
        )
    }
}
