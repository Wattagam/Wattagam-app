package com.wattagam.place

enum class VisitReasonCode {
    OUTSIDE_RADIUS,
    LOW_ACCURACY,
    STALE_LOCATION,
}

data class PlaceVisitResponse(
    val placeId: Long,
    val canWrite: Boolean,
    val reasonCode: VisitReasonCode?,
    val distanceMeters: Double?,
    val visitRadiusMeters: Int,
)
