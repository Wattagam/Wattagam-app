package com.wattagam.place

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.springframework.stereotype.Service

@Service
class LocationValidationService(
    private val properties: VisitProperties,
    private val clock: Clock,
) {

    fun validate(place: Place, request: PlaceVisitRequest): PlaceVisitResponse {
        validateCoordinates(request)
        val measuredAt = parseMeasuredAt(request.measuredAt)
        val now = clock.instant()
        if (measuredAt.isAfter(now.plus(properties.futureTolerance))) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        if (measuredAt.isBefore(now.minus(properties.maxAge))) {
            return response(place, VisitReasonCode.STALE_LOCATION)
        }
        if (request.accuracyMeters > properties.maxAccuracyMeters) {
            return response(place, VisitReasonCode.LOW_ACCURACY)
        }
        val distance = calculateDistanceMeters(place, request)
        val reason = if (distance <= place.visitRadiusMeters) null else VisitReasonCode.OUTSIDE_RADIUS
        return response(place, reason, distance)
    }

    private fun validateCoordinates(request: PlaceVisitRequest) {
        if (request.latitude < BigDecimal("-90") || request.latitude > BigDecimal("90") ||
            request.longitude < BigDecimal("-180") || request.longitude > BigDecimal("180") ||
            request.accuracyMeters < BigDecimal.ZERO
        ) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
    }

    private fun parseMeasuredAt(value: String): Instant = try {
        Instant.parse(value)
    } catch (_: DateTimeParseException) {
        throw ApiException(ErrorCode.INVALID_REQUEST)
    }

    private fun calculateDistanceMeters(place: Place, request: PlaceVisitRequest): Double {
        val latitude = Math.toRadians(place.latitude.toDouble())
        val currentLatitude = Math.toRadians(request.latitude.toDouble())
        val latitudeDelta = currentLatitude - latitude
        val longitudeDelta = Math.toRadians(request.longitude.toDouble() - place.longitude.toDouble())
        val haversine = sin(latitudeDelta / 2).pow(2) +
            cos(latitude) * cos(currentLatitude) * sin(longitudeDelta / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    private fun response(
        place: Place,
        reason: VisitReasonCode?,
        distance: Double? = null,
    ): PlaceVisitResponse = PlaceVisitResponse(
        placeId = requireNotNull(place.id),
        canWrite = reason == null,
        reasonCode = reason,
        distanceMeters = distance,
        visitRadiusMeters = place.visitRadiusMeters,
    )

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
