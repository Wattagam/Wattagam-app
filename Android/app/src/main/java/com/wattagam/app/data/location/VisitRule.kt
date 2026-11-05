package com.wattagam.app.data.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 현재 위치로 본 작성 가능 여부 (앱 쪽 1차 판정. 최종 판정은 서버) */
enum class VisitEligibility { Allowed, Outside, PermissionMissing, Unavailable }

/**
 * 작성 시작 위치 판정.
 * - 반경 30 m 는 임시 기준, 정확도 기준은 현장 검증 후 조정
 * - 이전 방문 기록은 쓰지 않고 매번 현재 위치로만 판정한다
 */
object VisitRule {
    const val RADIUS_METERS = 30.0
    const val MAX_ACCURACY_METERS = 50f

    fun evaluate(fix: LocationFix, placeLatitude: Double, placeLongitude: Double): VisitEligibility = when (fix) {
        LocationFix.PermissionMissing -> VisitEligibility.PermissionMissing
        LocationFix.Unavailable -> VisitEligibility.Unavailable
        is LocationFix.Found -> when {
            // 정확도가 낮으면 거리를 믿을 수 없어서 범위 밖으로 단정하지 않고 재시도를 안내
            fix.accuracyMeters > MAX_ACCURACY_METERS -> VisitEligibility.Unavailable
            distanceMeters(fix.latitude, fix.longitude, placeLatitude, placeLongitude) <= RADIUS_METERS -> VisitEligibility.Allowed
            else -> VisitEligibility.Outside
        }
    }

    /** 두 좌표 사이 거리 (하버사인, 미터) */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
