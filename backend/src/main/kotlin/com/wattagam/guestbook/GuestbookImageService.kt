package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import com.wattagam.storage.GuestbookImageStorage
import com.wattagam.storage.ImageStorageException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class GuestbookImageService(
    private val guestbookEntryRepository: GuestbookEntryRepository,
    private val imageStorage: ObjectProvider<GuestbookImageStorage>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** 모든 방명록이 전체 공개이므로 인증된 사용자는 누구나 조회할 수 있다. */
    fun read(entryId: Long): ByteArray {
        if (entryId <= 0) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        val entry = guestbookEntryRepository.findById(entryId).orElseThrow { ApiException(ErrorCode.GUESTBOOK_NOT_FOUND) }
        val storage = imageStorage.getIfAvailable() ?: throw ApiException(ErrorCode.GUESTBOOK_IMAGE_UNAVAILABLE)
        try {
            return storage.read(entry.imageObjectKey) ?: throw ApiException(ErrorCode.GUESTBOOK_NOT_FOUND)
        } catch (exception: ImageStorageException) {
            logger.warn("방명록 이미지를 읽지 못했습니다. 오류 유형: {}", exception.javaClass.simpleName)
            throw ApiException(ErrorCode.GUESTBOOK_IMAGE_UNAVAILABLE)
        }
    }
}
