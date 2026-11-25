package com.wattagam.app.feature.place

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.wattagam.app.R
import com.wattagam.app.data.location.CurrentLocationProvider
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.map.KakaoMapSupport
import com.wattagam.app.data.location.VisitEligibility
import com.wattagam.app.feature.capture.VisitCheckScreen
import com.wattagam.app.feature.capture.VisitCheckStatus
import com.wattagam.app.feature.capture.VisitCheckUiState
import com.wattagam.app.feature.capture.VisitMapIllustration
import com.wattagam.app.feature.map.KakaoCampusMap
import com.wattagam.app.feature.map.MapPlaceUiState
import com.wattagam.app.feature.map.MapPoint
import com.wattagam.app.feature.map.PLACE_ZOOM
import com.wattagam.app.ui.components.LocationUiState
import com.wattagam.app.ui.theme.WattagamShapes

/**
 * 04 · GPS 방문 확인 경로. 장소 설명·갤러리에서 범위 안으로 확인된 뒤에만 들어온다.
 * '다시 확인'은 현재 위치를 새로 측정하고, 범위 밖이면 04-E 로 바뀌어 '시작하기'가 사라진다.
 * 권한이 없거나 위치를 못 잡으면 범위 안이라고 볼 수 없으므로 04-E 와 함께 이유를 안내한다.
 */
@Composable
fun VisitCheckRoute(
    placeId: String,
    placeName: String,
    /** 장소 좌표. 지도를 이 장소 가까이 확대해서 보여준다 */
    latitude: Double,
    longitude: Double,
    visitorName: String,
    stampCount: Int,
    placeOrder: Int,
    placeStamped: Boolean,
    stampTotal: Int,
    judge: suspend (LocationFix.Found) -> VisitEligibility,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 지도에 찍을 내 위치: 들어올 때 한 번 재고, '다시 확인'할 때마다 그 측정값으로 옮긴다
    val context = LocalContext.current
    var myLocation by remember { mutableStateOf<MapPoint?>(null) }
    LaunchedEffect(Unit) {
        if (KakaoMapSupport.isAvailable) {
            val fix = CurrentLocationProvider(context).current()
            if (fix is LocationFix.Found && myLocation == null) myLocation = MapPoint(fix.latitude, fix.longitude)
        }
    }
    val check = rememberVisitCheck(
        judge = { fix -> myLocation = MapPoint(fix.latitude, fix.longitude); judge(fix) },
        onAllowed = {},
    )
    var mapFailed by remember { mutableStateOf(false) }
    val result = check.status
    // 들어온 직후는 범위 안. 다시 확인하는 동안에는 직전 판정을 그대로 보여준다 (04-E 에서 잠깐 04 로 바뀌지 않게)
    var outside by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(result) {
        when (result) {
            LocationUiState.Available -> outside = false
            LocationUiState.Outside, LocationUiState.Denied, LocationUiState.Unavailable, LocationUiState.Failed -> outside = true
            LocationUiState.Checking, null -> Unit
        }
    }

    VisitCheckScreen(
        state = VisitCheckUiState(
            placeName, visitorName, stampCount, if (outside) VisitCheckStatus.Outside else VisitCheckStatus.Available,
            placeOrder = placeOrder, placeStamped = placeStamped, stampTotal = stampTotal,
        ),
        onBack = onBack,
        onRecheck = check.start,
        onContinue = { if (!check.checking) onContinue() },
        onMap = onMap,
        modifier = modifier,
        mapContent = {
            if (KakaoMapSupport.isAvailable && !mapFailed) {
                KakaoCampusMap(
                    places = listOf(MapPlaceUiState(placeId, placeOrder, placeName, "", placeStamped, latitude, longitude)),
                    onPlaceClick = {},
                    onError = { mapFailed = true },
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(WattagamShapes.Card),
                    myLocation = myLocation,
                    center = MapPoint(latitude, longitude),
                    zoom = PLACE_ZOOM,
                )
            } else {
                VisitMapIllustration(placeName, outside)
            }
        },
    )

    // 범위 밖은 04-E 화면 자체가 안내한다. 권한·측정 실패는 이유가 달라 따로 알린다.
    var showProblem by remember { mutableStateOf(false) }
    LaunchedEffect(result) { showProblem = result == LocationUiState.Denied || result == LocationUiState.Unavailable || result == LocationUiState.Failed }
    val problem = when (result) {
        LocationUiState.Denied -> R.string.location_denied to R.string.location_denied_body
        LocationUiState.Unavailable -> R.string.location_unavailable to R.string.location_unavailable_body
        LocationUiState.Failed -> R.string.location_failed to R.string.location_failed_body
        else -> null
    }
    if (problem != null && showProblem) {
        AlertDialog(
            onDismissRequest = { showProblem = false },
            title = { Text(stringResource(problem.first)) },
            text = { Text(stringResource(problem.second)) },
            confirmButton = { TextButton(onClick = { showProblem = false }) { Text(stringResource(R.string.place_visit_ok)) } },
        )
    }
}
