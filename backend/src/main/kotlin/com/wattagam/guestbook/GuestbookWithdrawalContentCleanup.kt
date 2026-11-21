package com.wattagam.guestbook

import com.wattagam.storage.GuestbookImageStorage
import com.wattagam.user.WithdrawalContentCleanup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 탈퇴하는 사용자의 방명록 이미지를 OCI에서 지우고 방명록 행을 정리한다.
 * 탈퇴 트랜잭션(사용자 행 락 보유)에서 호출되며, 하나라도 실패하면 false를 반환해 탈퇴 전체를 롤백한다.
 */
@Component
class GuestbookWithdrawalContentCleanup(
    private val guestbookEntryRepository: GuestbookEntryRepository,
    private val imageStorage: ObjectProvider<GuestbookImageStorage>,
) : WithdrawalContentCleanup {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun deleteForUser(userId: Long): Boolean {
        val entries = guestbookEntryRepository.findAllByUserId(userId)
        if (entries.isEmpty()) {
            return true
        }
        val storage = imageStorage.getIfAvailable() ?: return false
        // 일부가 실패해도 나머지를 계속 지워 재시도 때 남은 작업을 줄인다. 이미 없는 이미지는 storage가 성공으로 처리한다.
        val failedCount = entries.count { !deleteImage(storage, it.imageObjectKey) }
        if (failedCount > 0) {
            logger.warn("탈퇴 정리에서 이미지 {}건을 삭제하지 못했습니다. 재시도가 필요합니다.", failedCount)
            return false
        }
        guestbookEntryRepository.deleteAll(entries)
        return true
    }

    private fun deleteImage(storage: GuestbookImageStorage, imageObjectKey: String): Boolean = try {
        storage.delete(imageObjectKey)
        true
    } catch (_: RuntimeException) {
        false
    }
}
