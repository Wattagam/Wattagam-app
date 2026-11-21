package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import com.wattagam.place.LocationValidationService
import com.wattagam.place.Place
import com.wattagam.place.PlaceRepository
import com.wattagam.place.VisitReasonCode
import com.wattagam.stamp.StampService
import com.wattagam.storage.GuestbookImageStorage
import com.wattagam.storage.ImageStorageException
import com.wattagam.user.UserRepository
import java.time.Clock
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class GuestbookService(
    private val userRepository: UserRepository,
    private val guestbookEntryRepository: GuestbookEntryRepository,
    private val placeRepository: PlaceRepository,
    private val locationValidationService: LocationValidationService,
    private val stampService: StampService,
    private val imageStorage: ObjectProvider<GuestbookImageStorage>,
    private val cleanupService: GuestbookImageCleanupService,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val transaction = TransactionTemplate(transactionManager)

    fun create(userId: Long, placeId: Long, command: GuestbookCreateCommand): GuestbookCreateResult {
        validateInput(placeId, command)
        val place = placeRepository.findById(placeId).orElseThrow { ApiException(ErrorCode.PLACE_NOT_FOUND) }
        val storage = imageStorage.getIfAvailable() ?: throw ApiException(ErrorCode.GUESTBOOK_IMAGE_UNAVAILABLE)
        val attemptedKeys = mutableListOf<String>()
        try {
            return requireNotNull(
                transaction.execute { createInTransaction(userId, place, command, storage, attemptedKeys) },
            )
        } catch (exception: Throwable) {
            cleanUpUnpublishedImages(storage, attemptedKeys)
            throw exception
        }
    }

    private fun validateInput(placeId: Long, command: GuestbookCreateCommand) {
        if (placeId <= 0 || command.clientRequestId.isBlank() || command.clientRequestId.length > MAX_REQUEST_ID_LENGTH) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        if (command.message != null && command.message.codePointCount(0, command.message.length) > MAX_MESSAGE_LENGTH) {
            throw ApiException(ErrorCode.GUESTBOOK_MESSAGE_TOO_LONG)
        }
    }

    // 탈퇴와 같은 사용자 행 락을 먼저 잡고, 락은 이 트랜잭션이 끝날 때까지 유지한다.
    private fun createInTransaction(
        userId: Long,
        place: Place,
        command: GuestbookCreateCommand,
        storage: GuestbookImageStorage,
        attemptedKeys: MutableList<String>,
    ): GuestbookCreateResult {
        userRepository.findByIdForUpdate(userId) ?: throw ApiException(ErrorCode.UNAUTHORIZED)
        val placeId = requireNotNull(place.id)
        val existing = guestbookEntryRepository.findByUserIdAndClientRequestId(userId, command.clientRequestId)
        if (existing != null) {
            return replay(existing, placeId)
        }
        requireWritableLocation(place, command)
        val imageKey = storeImage(storage, command.image, attemptedKeys)
        val stampAlreadyEarned = placeId in stampService.getEarnedPlaceIds(userId)
        val entry = saveEntry(userId, placeId, imageKey, command)
        stampService.grantForPublishedEntry(userId, placeId, requireNotNull(entry.id))
        return GuestbookCreateResult(toResponse(entry, stampAwarded = !stampAlreadyEarned), created = true)
    }

    // 같은 요청 ID를 다른 장소에 재사용하면 기존 방명록을 잘못 돌려주지 않도록 거절한다.
    private fun replay(existing: GuestbookEntry, placeId: Long): GuestbookCreateResult {
        if (existing.placeId != placeId) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        return GuestbookCreateResult(toResponse(existing, stampAwarded = false), created = false)
    }

    private fun requireWritableLocation(place: Place, command: GuestbookCreateCommand) {
        val visit = locationValidationService.validate(place, command.location)
        if (visit.canWrite) {
            return
        }
        throw ApiException(
            when (visit.reasonCode) {
                VisitReasonCode.LOW_ACCURACY -> ErrorCode.GUESTBOOK_LOW_ACCURACY
                VisitReasonCode.STALE_LOCATION -> ErrorCode.GUESTBOOK_STALE_LOCATION
                else -> ErrorCode.GUESTBOOK_OUTSIDE_RADIUS
            },
        )
    }

    /**
     * 이미 존재하는 키(put이 false)는 우리가 만든 객체가 아니므로 정리 대상에서 빼고 새 키로 다시 저장한다.
     * 응답을 받지 못한 실패는 객체가 남았을 수 있어 시도한 키를 보존한다.
     */
    private fun storeImage(storage: GuestbookImageStorage, image: ByteArray, attemptedKeys: MutableList<String>): String {
        repeat(MAX_KEY_ATTEMPTS) {
            val key = storage.createKey()
            attemptedKeys += key
            try {
                if (storage.put(key, image)) {
                    return key
                }
            } catch (exception: ImageStorageException) {
                logger.warn("방명록 이미지 저장에 실패했습니다. 오류 유형: {}", exception.javaClass.simpleName)
                throw ApiException(ErrorCode.GUESTBOOK_IMAGE_UNAVAILABLE)
            }
            attemptedKeys.remove(key)
        }
        throw ApiException(ErrorCode.GUESTBOOK_IMAGE_UNAVAILABLE)
    }

    private fun saveEntry(userId: Long, placeId: Long, imageKey: String, command: GuestbookCreateCommand): GuestbookEntry {
        val now = clock.instant().truncatedTo(ChronoUnit.MICROS)
        return guestbookEntryRepository.save(
            GuestbookEntry(
                placeId = placeId,
                userId = userId,
                imageObjectKey = imageKey,
                message = command.message,
                clientRequestId = command.clientRequestId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private fun toResponse(entry: GuestbookEntry, stampAwarded: Boolean): GuestbookCreateResponse {
        val entryId = requireNotNull(entry.id)
        return GuestbookCreateResponse(
            entryId = entryId,
            placeId = entry.placeId,
            imageUrl = "/api/guestbook/$entryId/image",
            message = entry.message,
            createdAt = entry.createdAt,
            stampAwarded = stampAwarded,
            earnedCount = stampService.getEarnedPlaceIds(entry.userId).size,
            totalCount = placeRepository.count().toInt(),
        )
    }

    // 커밋 여부를 알 수 없을 때 방명록이 이미 참조하는 이미지를 지우지 않도록 DB를 먼저 확인한다.
    private fun cleanUpUnpublishedImages(storage: GuestbookImageStorage, keys: List<String>) {
        keys.forEach { key ->
            try {
                if (!guestbookEntryRepository.existsByImageObjectKey(key)) {
                    deleteOrReserve(storage, key)
                }
            } catch (exception: RuntimeException) {
                logger.warn("저장한 방명록 이미지를 정리하지 못했습니다. 오류 유형: {}", exception.javaClass.simpleName)
            }
        }
    }

    // 삭제에 실패한 객체는 키를 DB에 보존해 주기 작업이 다시 정리하게 한다.
    private fun deleteOrReserve(storage: GuestbookImageStorage, key: String) {
        try {
            storage.delete(key)
        } catch (exception: ImageStorageException) {
            logger.warn("저장한 방명록 이미지를 바로 정리하지 못했습니다. 오류 유형: {}", exception.javaClass.simpleName)
            cleanupService.record(key)
        }
    }

    companion object {
        private const val MAX_MESSAGE_LENGTH = 80
        private const val MAX_REQUEST_ID_LENGTH = 64
        private const val MAX_KEY_ATTEMPTS = 3
    }
}
