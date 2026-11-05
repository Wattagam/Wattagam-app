package com.wattagam.app.feature.place

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.wattagam.app.data.location.CurrentLocationProvider
import com.wattagam.app.data.location.VisitEligibility
import com.wattagam.app.data.location.VisitRule
import com.wattagam.app.ui.components.LocationUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 작성 시작 위치 확인 상태. [status] 가 null 이면 아직 확인 전 */
@Stable
class VisitCheck internal constructor(
    private val statusState: MutableState<LocationUiState?>,
    /** '방명록 쓰기' 버튼에서 호출 */
    val start: () -> Unit,
) {
    val status: LocationUiState? get() = statusState.value
    val checking: Boolean get() = statusState.value == LocationUiState.Checking
}

/**
 * '방명록 쓰기'를 누를 때마다 현재 위치를 새로 확인한다 (이전 방문은 작성 자격이 아님).
 * 권한이 없으면 그때 요청하고, 범위 안이면 [onAllowed] 를 호출한다.
 * 최종 작성 자격·도장 지급은 등록할 때 서버가 다시 판정한다.
 */
@Composable
fun rememberVisitCheck(latitude: Double, longitude: Double, onAllowed: () -> Unit): VisitCheck {
    val context = LocalContext.current
    val provider = remember(context) { CurrentLocationProvider(context) }
    val scope = rememberCoroutineScope()
    val currentOnAllowed by rememberUpdatedState(onAllowed)
    val statusState = remember { mutableStateOf<LocationUiState?>(null) }
    val jobState = remember { mutableStateOf<Job?>(null) }

    fun measure() {
        if (jobState.value?.isActive == true) return // 확인 중 중복 실행 방지
        statusState.value = LocationUiState.Checking
        jobState.value = scope.launch {
            val result = VisitRule.evaluate(provider.current(), latitude, longitude)
            statusState.value = when (result) {
                VisitEligibility.Allowed -> LocationUiState.Available
                VisitEligibility.Outside -> LocationUiState.Outside
                VisitEligibility.PermissionMissing -> LocationUiState.Denied
                VisitEligibility.Unavailable -> LocationUiState.Unavailable
            }
            if (result == VisitEligibility.Allowed) currentOnAllowed()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) measure() else statusState.value = LocationUiState.Denied
    }

    return VisitCheck(statusState) {
        if (provider.hasPreciseLocationPermission()) {
            measure()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
