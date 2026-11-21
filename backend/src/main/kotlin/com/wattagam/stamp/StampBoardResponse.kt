package com.wattagam.stamp

import java.time.Instant

data class StampBoardResponse(
    val earnedCount: Int,
    val totalCount: Int,
    val stamps: List<StampSlotResponse>,
)

data class StampSlotResponse(
    val placeId: Long,
    val courseOrder: Int,
    val placeName: String,
    val earned: Boolean,
    val earnedAt: Instant?,
    val firstEntryId: Long?,
)
