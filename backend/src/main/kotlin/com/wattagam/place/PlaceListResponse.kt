package com.wattagam.place

import java.math.BigDecimal

data class PlaceListResponse(
    val places: List<PlaceSummaryResponse>,
)

data class PlaceSummaryResponse(
    val placeId: Long,
    val courseOrder: Int,
    val name: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val visitRadiusMeters: Int,
)
