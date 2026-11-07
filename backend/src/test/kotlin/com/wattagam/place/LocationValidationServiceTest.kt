package com.wattagam.place

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationValidationServiceTest {

    private val now = Instant.parse("2026-11-07T05:00:00Z")
    private val service = LocationValidationService(
        VisitProperties(
            maxAccuracyMeters = BigDecimal("20"),
            maxAge = Duration.ofSeconds(60),
            futureTolerance = Duration.ofSeconds(10),
        ),
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun acceptsTheCenterOfEachPlace() {
        listOf(75, 40, 30).forEach { radius ->
            val place = createPlace(radius)
            val response = service.validate(place, request())

            assertTrue(response.canWrite)
            assertNull(response.reasonCode)
            assertEquals(place.id, response.placeId)
            assertEquals(radius, response.visitRadiusMeters)
            assertEquals(0.0, requireNotNull(response.distanceMeters), 0.001)
        }
    }

    @Test
    fun appliesEachPlacesRadiusInsteadOfOneGlobalThirtyMeterLimit() {
        val thirtyFiveMetersAway = request(latitude = latitudeNorthOfCenter(35.0))
        assertTrue(service.validate(createPlace(75), thirtyFiveMetersAway).canWrite)
        assertTrue(service.validate(createPlace(40), thirtyFiveMetersAway).canWrite)
        val pond = service.validate(createPlace(30), thirtyFiveMetersAway)
        assertFalse(pond.canWrite)
        assertEquals(VisitReasonCode.OUTSIDE_RADIUS, pond.reasonCode)
        assertEquals(35.0, requireNotNull(pond.distanceMeters), 0.01)

        val sixtyMetersAway = request(latitude = latitudeNorthOfCenter(60.0))
        assertTrue(service.validate(createPlace(75), sixtyMetersAway).canWrite)
        assertFalse(service.validate(createPlace(40), sixtyMetersAway).canWrite)
    }

    @Test
    fun distinguishesLocationsImmediatelyInsideAndOutsideEachRadius() {
        listOf(75, 40, 30).forEach { radius ->
            val place = createPlace(radius)
            val inside = service.validate(place, request(latitude = latitudeNorthOfCenter(radius - 0.01)))
            val outside = service.validate(place, request(latitude = latitudeNorthOfCenter(radius + 0.01)))

            assertTrue(inside.canWrite, "반경 ${radius}m 안쪽 위치는 허용되어야 합니다.")
            assertNull(inside.reasonCode)
            assertFalse(outside.canWrite, "반경 ${radius}m 바깥 위치는 거절되어야 합니다.")
            assertEquals(VisitReasonCode.OUTSIDE_RADIUS, outside.reasonCode)
        }
    }

    @Test
    fun acceptsTheAccuracyThresholdAndRejectsWorseAccuracyEvenAtTheCenter() {
        val place = createPlace()
        assertTrue(service.validate(place, request(accuracyMeters = BigDecimal("20"))).canWrite)

        val inaccurate = service.validate(place, request(accuracyMeters = BigDecimal("20.01")))
        assertFalse(inaccurate.canWrite)
        assertEquals(VisitReasonCode.LOW_ACCURACY, inaccurate.reasonCode)
        assertTrue(service.validate(place, request(accuracyMeters = BigDecimal.ZERO)).canWrite)
    }

    @Test
    fun acceptsTheAgeThresholdAndRejectsAnOlderLocation() {
        val place = createPlace()
        assertTrue(service.validate(place, request(measuredAt = now.minusSeconds(60).toString())).canWrite)

        val stale = service.validate(place, request(measuredAt = now.minusSeconds(60).minusMillis(1).toString()))
        assertFalse(stale.canWrite)
        assertEquals(VisitReasonCode.STALE_LOCATION, stale.reasonCode)
    }

    @Test
    fun reportsStaleLocationBeforePoorAccuracyOrAnOutsideLocation() {
        val response = service.validate(
            createPlace(),
            request(
                latitude = latitudeNorthOfCenter(100.0),
                accuracyMeters = BigDecimal("50"),
                measuredAt = now.minusSeconds(61).toString(),
            ),
        )
        assertFalse(response.canWrite)
        assertEquals(VisitReasonCode.STALE_LOCATION, response.reasonCode)
    }

    @Test
    fun reportsPoorAccuracyBeforeDistanceOutsideTheRadius() {
        val response = service.validate(
            createPlace(),
            request(latitude = latitudeNorthOfCenter(100.0), accuracyMeters = BigDecimal("21")),
        )
        assertFalse(response.canWrite)
        assertEquals(VisitReasonCode.LOW_ACCURACY, response.reasonCode)
    }

    @Test
    fun allowsClockSkewThroughTheFutureToleranceAndRejectsAnythingLater() {
        val place = createPlace()
        assertTrue(service.validate(place, request(measuredAt = now.plusSeconds(10).toString())).canWrite)
        expectInvalid(request(measuredAt = now.plusSeconds(10).plusMillis(1).toString()))
    }

    @Test
    fun rejectsInvalidCoordinatesNegativeAccuracyAndUnreadableTimestamps() {
        listOf(
            request(latitude = BigDecimal("90.00001")),
            request(latitude = BigDecimal("-90.00001")),
            request(longitude = BigDecimal("180.00001")),
            request(longitude = BigDecimal("-180.00001")),
            request(accuracyMeters = BigDecimal("-0.01")),
            request(measuredAt = ""),
            request(measuredAt = "2026-11-07T05:00:00"),
            request(measuredAt = "not-a-time"),
        ).forEach(::expectInvalid)
    }

    @Test
    fun acceptsValidCoordinateBoundsAsOutsideLocationsRatherThanMalformedRequests() {
        listOf(
            request(latitude = BigDecimal("90")),
            request(latitude = BigDecimal("-90")),
            request(longitude = BigDecimal("180")),
            request(longitude = BigDecimal("-180")),
        ).forEach { location ->
            val response = service.validate(createPlace(), location)
            assertFalse(response.canWrite)
            assertEquals(VisitReasonCode.OUTSIDE_RADIUS, response.reasonCode)
            assertTrue(requireNotNull(response.distanceMeters).isFinite())
        }
    }

    @Test
    fun computesFiniteDistanceAcrossTheDateLine() {
        val place = createPlace(75, latitude = BigDecimal.ZERO, longitude = BigDecimal("179.9999"))
        val response = service.validate(
            place,
            request(latitude = BigDecimal.ZERO, longitude = BigDecimal("-179.9999")),
        )
        assertTrue(response.canWrite)
        assertEquals(22.239, requireNotNull(response.distanceMeters), 0.01)
    }

    private fun expectInvalid(location: PlaceVisitRequest) {
        val exception = assertFailsWith<ApiException> { service.validate(createPlace(), location) }
        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    private fun createPlace(
        radius: Int = 30,
        latitude: BigDecimal = BigDecimal("37.5"),
        longitude: BigDecimal = BigDecimal("126.95"),
    ): Place = Place(
        id = 1L,
        courseOrder = 1,
        name = "테스트 장소",
        description = "위치 검증용 가상 장소",
        highlight = "테스트 포인트",
        directions = "테스트 경로",
        visitPointName = "테스트 중심점",
        latitude = latitude,
        longitude = longitude,
        visitRadiusMeters = radius,
    )

    private fun request(
        latitude: BigDecimal = BigDecimal("37.5"),
        longitude: BigDecimal = BigDecimal("126.95"),
        accuracyMeters: BigDecimal = BigDecimal("10"),
        measuredAt: String = now.toString(),
    ): PlaceVisitRequest = PlaceVisitRequest(latitude, longitude, accuracyMeters, measuredAt)

    private fun latitudeNorthOfCenter(distanceMeters: Double): BigDecimal =
        BigDecimal.valueOf(37.5 + Math.toDegrees(distanceMeters / 6_371_000.0))
}
