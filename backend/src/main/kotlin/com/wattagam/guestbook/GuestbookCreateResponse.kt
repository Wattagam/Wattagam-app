package com.wattagam.guestbook

import java.time.Instant

data class GuestbookCreateResponse(
    val entryId: Long,
    val placeId: Long,
    val imageUrl: String,
    val message: String?,
    val createdAt: Instant,
    val stampAwarded: Boolean,
    val earnedCount: Int,
    val totalCount: Int,
)

/** 새로 저장했는지(201)와 기존 결과 재반환인지(200)를 함께 전달한다. */
data class GuestbookCreateResult(val response: GuestbookCreateResponse, val created: Boolean)
