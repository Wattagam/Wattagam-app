package com.wattagam.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 한 번 측정한 현재 위치 결과 */
sealed interface LocationFix {
    /** [measuredAtMillis] 는 위치를 잰 시각 (기기 시계 기준). 서버는 60초 지난 위치를 받지 않는다 */
    data class Found(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val measuredAtMillis: Long = System.currentTimeMillis(),
    ) : LocationFix
    /** 정확한 위치 권한이 없음 (대략적인 위치만 허용한 경우 포함) */
    data object PermissionMissing : LocationFix
    /** 위치 서비스가 꺼져 있거나 측정에 실패함 */
    data object Unavailable : LocationFix
}

/**
 * 작성 시작·등록 직전에 한 번만 현재 위치를 측정한다.
 * 연속 위치 수집·백그라운드 위치는 하지 않으며, 좌표를 로그에 남기지 않는다.
 */
class CurrentLocationProvider(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    fun hasPreciseLocationPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // 아래에서 권한을 먼저 확인한다
    suspend fun current(): LocationFix {
        if (!hasPreciseLocationPermission()) return LocationFix.PermissionMissing
        if (!isLocationEnabled()) return LocationFix.Unavailable

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_FIX_AGE_MILLIS) // 오래된 캐시 위치는 쓰지 않음
            .setDurationMillis(TIMEOUT_MILLIS)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            client.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    val fix = if (location == null) {
                        LocationFix.Unavailable
                    } else {
                        // 위치를 잰 지 얼마나 지났는지(부팅 후 경과 시간 기준)로 측정 시각을 구한다
                        val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
                        LocationFix.Found(location.latitude, location.longitude, location.accuracy, System.currentTimeMillis() - ageMillis.coerceAtLeast(0))
                    }
                    if (continuation.isActive) continuation.resume(fix)
                }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(LocationFix.Unavailable) }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private companion object {
        // 임시 기준. 현장 검증 후 조정
        const val MAX_FIX_AGE_MILLIS = 10_000L
        const val TIMEOUT_MILLIS = 15_000L
    }
}
