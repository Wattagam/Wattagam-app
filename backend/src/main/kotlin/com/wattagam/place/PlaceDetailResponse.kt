package com.wattagam.place

import java.math.BigDecimal

data class PlaceDetailResponse(
    val placeId: Long,
    val courseOrder: Int,
    val name: String,
    val description: String,
    val highlight: String,
    val directions: String,
    val visitPointName: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val visitRadiusMeters: Int,
)
