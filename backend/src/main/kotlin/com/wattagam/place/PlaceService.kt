package com.wattagam.place

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import com.wattagam.stamp.StampService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceService(
    private val placeRepository: PlaceRepository,
    private val locationValidationService: LocationValidationService,
    private val stampService: StampService,
) {

    fun getPlaces(userId: Long): PlaceListResponse {
        val earnedPlaceIds = stampService.getEarnedPlaceIds(userId)
        val places = placeRepository.findAllByOrderByCourseOrderAsc().map { place ->
            val placeId = requireNotNull(place.id)
            PlaceSummaryResponse(
                placeId = placeId,
                courseOrder = place.courseOrder,
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                visitRadiusMeters = place.visitRadiusMeters,
                stampEarned = placeId in earnedPlaceIds,
            )
        }
        return PlaceListResponse(places)
    }

    fun getPlace(id: Long): PlaceDetailResponse {
        val place = findPlace(id)
        return PlaceDetailResponse(
            placeId = requireNotNull(place.id),
            courseOrder = place.courseOrder,
            name = place.name,
            description = place.description,
            highlight = place.highlight,
            directions = place.directions,
            visitPointName = place.visitPointName,
            latitude = place.latitude,
            longitude = place.longitude,
            visitRadiusMeters = place.visitRadiusMeters,
        )
    }

    fun checkVisit(id: Long, request: PlaceVisitRequest): PlaceVisitResponse =
        locationValidationService.validate(findPlace(id), request)

    private fun findPlace(id: Long): Place {
        if (id <= 0) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        return placeRepository.findById(id).orElseThrow { ApiException(ErrorCode.PLACE_NOT_FOUND) }
    }
}
