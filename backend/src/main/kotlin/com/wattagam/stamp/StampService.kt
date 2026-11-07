package com.wattagam.stamp

import com.wattagam.place.PlaceRepository
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StampService(
    private val stampRepository: StampRepository,
    private val placeRepository: PlaceRepository,
    private val clock: Clock,
) {

    fun getBoard(userId: Long): StampBoardResponse {
        val earnedByPlace = stampRepository.findAllByUserId(userId).associateBy { it.placeId }
        val slots = placeRepository.findAllByOrderByCourseOrderAsc().map { place ->
            val placeId = requireNotNull(place.id)
            val stamp = earnedByPlace[placeId]
            StampSlotResponse(placeId, place.courseOrder, place.name, stamp != null, stamp?.earnedAt, stamp?.firstEntryId)
        }
        return StampBoardResponse(slots.count { it.earned }, slots.size, slots)
    }

    fun getEarnedPlaceIds(userId: Long): Set<Long> =
        stampRepository.findAllByUserId(userId).mapTo(mutableSetOf()) { it.placeId }

    @Transactional(propagation = Propagation.MANDATORY)
    fun grantForPublishedEntry(userId: Long, placeId: Long, entryId: Long) {
        require(entryId > 0) { "최초 방명록 ID는 양수여야 합니다." }
        stampRepository.insertIfMissing(userId, placeId, entryId, clock.instant())
    }
}
