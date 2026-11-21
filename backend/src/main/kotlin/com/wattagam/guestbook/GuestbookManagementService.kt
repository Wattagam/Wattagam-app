package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import com.wattagam.user.UserRepository
import java.time.Clock
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class GuestbookManagementService(
    private val userRepository: UserRepository,
    private val guestbookEntryRepository: GuestbookEntryRepository,
    private val cleanupService: GuestbookImageCleanupService,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager)

    /** 메시지만 수정한다. 이미지와 도장은 바꾸지 않는다. */
    fun updateMessage(userId: Long, entryId: Long, message: String?): GuestbookDetailResponse {
        val normalized = message?.takeIf { it.isNotEmpty() }
        if (normalized != null && normalized.codePointCount(0, normalized.length) > MAX_MESSAGE_LENGTH) {
            throw ApiException(ErrorCode.GUESTBOOK_MESSAGE_TOO_LONG)
        }
        return requireNotNull(
            transaction.execute {
                val entry = findOwnedEntry(userId, entryId)
                entry.message = normalized
                entry.updatedAt = clock.instant().truncatedTo(ChronoUnit.MICROS)
                guestbookEntryRepository.saveAndFlush(entry)
                val row = guestbookEntryRepository.findDetailRow(entryId) ?: throw ApiException(ErrorCode.GUESTBOOK_NOT_FOUND)
                GuestbookQueryService.toDetail(row, userId)
            },
        )
    }

    /**
     * 방명록 행을 지우고 도장은 유지한다(`first_entry_id`는 DB가 NULL로 바꾼다).
     * 이미지 키는 같은 트랜잭션에서 보존해 두므로, 저장소 삭제가 실패해도 응답은 성공이고 나중에 재정리한다.
     */
    fun delete(userId: Long, entryId: Long) {
        val imageKey = requireNotNull(
            transaction.execute {
                val entry = findOwnedEntry(userId, entryId)
                cleanupService.reserve(entry.imageObjectKey)
                guestbookEntryRepository.delete(entry)
                entry.imageObjectKey
            },
        )
        cleanupService.deleteNow(imageKey)
    }

    // 탈퇴와 같은 사용자 행 락을 먼저 잡아 탈퇴 정리와 동시에 같은 방명록을 건드리지 않게 한다.
    private fun findOwnedEntry(userId: Long, entryId: Long): GuestbookEntry {
        if (entryId <= 0) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        userRepository.findByIdForUpdate(userId) ?: throw ApiException(ErrorCode.UNAUTHORIZED)
        val entry = guestbookEntryRepository.findById(entryId).orElseThrow { ApiException(ErrorCode.GUESTBOOK_NOT_FOUND) }
        if (entry.userId != userId) {
            throw ApiException(ErrorCode.FORBIDDEN)
        }
        return entry
    }

    companion object {
        private const val MAX_MESSAGE_LENGTH = 80
    }
}
