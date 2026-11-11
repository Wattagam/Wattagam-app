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
import com.wattagam.app.data.AccountProblem
import com.wattagam.app.data.location.CurrentLocationProvider
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.location.VisitEligibility
import com.wattagam.app.ui.components.LocalAccountProblemHandler
import com.wattagam.app.ui.components.LocationUiState
import kotlinx.coroutines.CancellationException
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
 * '방명록 쓰기'를 누를 때마다 현재 위치를 새로 재서 [judge] 에 묻는다 (이전 방문은 작성 자격이 아님).
 * [judge] 는 서버 연결 시 방문 확인 API, 샘플이면 앱 쪽 반경 판정이다.
 * 권한이 없으면 그때 요청하고, 쓸 수 있으면 잰 위치로 [onAllowed] 를 호출한다.
 * 최종 작성 자격·도장 지급은 등록할 때 서버가 다시 판정한다.
 */
@Composable
fun rememberVisitCheck(judge: suspend (LocationFix.Found) -> VisitEligibility, onAllowed: (LocationFix.Found) -> Unit): VisitCheck {
    val context = LocalContext.current
    val provider = remember(context) { CurrentLocationProvider(context) }
    val scope = rememberCoroutineScope()
    val currentJudge by rememberUpdatedState(judge)
    val currentOnAllowed by rememberUpdatedState(onAllowed)
    val onAccountProblem by rememberUpdatedState(LocalAccountProblemHandler.current)
    val statusState = remember { mutableStateOf<LocationUiState?>(null) }
    val jobState = remember { mutableStateOf<Job?>(null) }

    fun measure() {
        if (jobState.value?.isActive == true) return // 확인 중 중복 실행 방지
        statusState.value = LocationUiState.Checking
        jobState.value = scope.launch {
            val fix = provider.current()
            val result = when (fix) {
                LocationFix.PermissionMissing -> VisitEligibility.PermissionMissing
                LocationFix.Unavailable -> VisitEligibility.Unavailable
                is LocationFix.Found -> try {
                    currentJudge(fix)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: AccountProblem) {
                    statusState.value = null
                    onAccountProblem(e)
                    return@launch
                } catch (_: Exception) {
                    // 서버에 닿지 못했거나 서버 오류: 범위 밖으로 단정하지 않고 다시 시도하게 한다
                    statusState.value = LocationUiState.Failed
                    return@launch
                }
            }
            statusState.value = when (result) {
                VisitEligibility.Allowed -> LocationUiState.Available
                VisitEligibility.Outside -> LocationUiState.Outside
                VisitEligibility.PermissionMissing -> LocationUiState.Denied
                VisitEligibility.Unavailable -> LocationUiState.Unavailable
            }
            if (result == VisitEligibility.Allowed && fix is LocationFix.Found) currentOnAllowed(fix)
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
