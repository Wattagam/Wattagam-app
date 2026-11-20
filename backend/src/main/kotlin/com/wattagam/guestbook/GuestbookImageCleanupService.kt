package com.wattagam.guestbook

import com.wattagam.storage.GuestbookImageStorage
import java.time.Clock
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** OCI 이미지 삭제가 실패해도 키를 잃지 않도록 DB에 보존하고, 성공할 때까지 재시도한다. */
@Service
class GuestbookImageCleanupService(
    private val cleanupRepository: GuestbookImageCleanupRepository,
    private val imageStorage: ObjectProvider<GuestbookImageStorage>,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** 호출자 트랜잭션에 참여해, 방명록 삭제가 커밋될 때만 정리 대상이 남게 한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    fun reserve(imageObjectKey: String) {
        cleanupRepository.save(GuestbookImageCleanup(imageObjectKey = imageObjectKey, createdAt = now()))
    }

    /** 삭제에 실패한 키를 별도 트랜잭션으로 보존한다. DB가 실패해도 원래 흐름을 막지 않는다. */
    fun record(imageObjectKey: String) {
        try {
            if (!cleanupRepository.existsByImageObjectKey(imageObjectKey)) {
                cleanupRepository.save(GuestbookImageCleanup(imageObjectKey = imageObjectKey, createdAt = now()))
            }
        } catch (_: DataIntegrityViolationException) {
            // 동시에 같은 키가 보존됐다면 이미 재시도 대상이다.
        } catch (exception: RuntimeException) {
            logger.warn("이미지 정리 대상을 보존하지 못했습니다. 오류 유형: {}", exception.javaClass.simpleName)
        }
    }

    /** 이미지를 지우고 보존 행도 제거한다. 실패하면 행을 남기고 false를 반환한다. */
    fun deleteNow(imageObjectKey: String): Boolean {
        val storage = imageStorage.getIfAvailable() ?: return false
        return try {
            storage.delete(imageObjectKey)
            cleanupRepository.deleteByKey(imageObjectKey)
            true
        } catch (exception: RuntimeException) {
            logger.warn("방명록 이미지 정리를 재시도해야 합니다. 오류 유형: {}", exception.javaClass.simpleName)
            false
        }
    }

    @Scheduled(
        fixedDelayString = "\${app.guestbook.cleanup.retry-interval:PT5M}",
        initialDelayString = "\${app.guestbook.cleanup.retry-interval:PT5M}",
    )
    fun retryPending() {
        cleanupRepository.findTop50ByOrderByIdAsc().forEach { deleteNow(it.imageObjectKey) }
    }

    private fun now() = clock.instant().truncatedTo(ChronoUnit.MICROS)
}
