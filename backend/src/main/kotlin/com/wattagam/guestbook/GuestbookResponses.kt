package com.wattagam.guestbook

import java.time.Instant

data class GuestbookGalleryResponse(
    val entries: List<GuestbookGalleryItemResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

data class GuestbookGalleryItemResponse(
    val entryId: Long,
    val nickname: String,
    val imageUrl: String,
    val message: String?,
    val createdAt: Instant,
    val edited: Boolean,
    val mine: Boolean,
)

data class GuestbookDetailResponse(
    val entryId: Long,
    val placeId: Long,
    val placeName: String,
    val nickname: String,
    val imageUrl: String,
    val message: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val edited: Boolean,
    val mine: Boolean,
)

data class GuestbookMyListResponse(
    val entries: List<GuestbookMyItemResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

data class GuestbookMyItemResponse(
    val entryId: Long,
    val placeId: Long,
    val placeName: String,
    val imageUrl: String,
    val message: String?,
    val createdAt: Instant,
    val edited: Boolean,
)

fun imageUrlOf(entryId: Long): String = "/api/guestbook/$entryId/image"
