package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import com.wattagam.place.PlaceRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GuestbookQueryService(
    private val guestbookEntryRepository: GuestbookEntryRepository,
    private val placeRepository: PlaceRepository,
) {

    fun getGallery(userId: Long, placeId: Long, page: Int, size: Int): GuestbookGalleryResponse {
        if (placeId <= 0) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        val pageable = toPageable(page, size)
        if (!placeRepository.existsById(placeId)) {
            throw ApiException(ErrorCode.PLACE_NOT_FOUND)
        }
        val slice = guestbookEntryRepository.findGalleryRows(placeId, pageable)
        val entries = slice.content.map { row ->
            GuestbookGalleryItemResponse(
                entryId = row.entryId,
                nickname = row.nickname.orEmpty(),
                imageUrl = imageUrlOf(row.entryId),
                message = row.message,
                createdAt = row.createdAt,
                edited = row.edited,
                mine = row.authorId == userId,
            )
        }
        return GuestbookGalleryResponse(entries, page, size, slice.hasNext())
    }

    fun getDetail(userId: Long, entryId: Long): GuestbookDetailResponse {
        if (entryId <= 0) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        val row = guestbookEntryRepository.findDetailRow(entryId) ?: throw ApiException(ErrorCode.GUESTBOOK_NOT_FOUND)
        return toDetail(row, userId)
    }

    fun getMine(userId: Long, page: Int, size: Int): GuestbookMyListResponse {
        val slice = guestbookEntryRepository.findRowsByAuthor(userId, toPageable(page, size))
        val entries = slice.content.map { row ->
            GuestbookMyItemResponse(
                entryId = row.entryId,
                placeId = row.placeId,
                placeName = row.placeName,
                imageUrl = imageUrlOf(row.entryId),
                message = row.message,
                createdAt = row.createdAt,
                edited = row.edited,
            )
        }
        return GuestbookMyListResponse(entries, page, size, slice.hasNext())
    }

    private fun toPageable(page: Int, size: Int): PageRequest {
        if (page < 0 || size !in 1..MAX_PAGE_SIZE) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        return PageRequest.of(page, size)
    }

    companion object {
        const val MAX_PAGE_SIZE = 50
        const val DEFAULT_PAGE_SIZE = 20

        fun toDetail(row: GuestbookRow, userId: Long): GuestbookDetailResponse = GuestbookDetailResponse(
            entryId = row.entryId,
            placeId = row.placeId,
            placeName = row.placeName,
            nickname = row.nickname.orEmpty(),
            imageUrl = imageUrlOf(row.entryId),
            message = row.message,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            edited = row.edited,
            mine = row.authorId == userId,
        )
    }
}
