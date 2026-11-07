package com.wattagam.place

import com.wattagam.auth.JwtService
import com.wattagam.user.User
import com.wattagam.user.UserRepository
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals

@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.auth.kakao.app-id=12345",
        "app.visit.max-accuracy-meters=20",
        "app.visit.max-age=60s",
        "app.visit.future-tolerance=10s",
    ],
)
@AutoConfigureMockMvc
@Testcontainers
class PlaceIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var placeRepository: PlaceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    private lateinit var accessToken: String
    private lateinit var library: Place

    @BeforeEach
    fun preparePlacesAndAnOnboardedUser() {
        placeRepository.deleteAll()
        userRepository.deleteAll()
        placeRepository.save(createPlace(3, "청룡연못", 30))
        placeRepository.save(createPlace(1, "310관", 75))
        library = placeRepository.save(createPlace(2, "도서관", 40))
        val user = userRepository.save(User(kakaoUserId = 123456789L, nickname = "푸앙이", createdAt = Instant.now()))
        accessToken = jwtService.issueAccessToken(requireNotNull(user.id))
    }

    @Test
    fun listsThreePlacesByCourseOrderWithTheirOwnVisitRadiiWithoutLocationInput() {
        mockMvc.perform(get("/api/places").header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("PLACE_200_1"))
            .andExpect(jsonPath("$.result.places.length()").value(3))
            .andExpect(jsonPath("$.result.places[0].courseOrder").value(1))
            .andExpect(jsonPath("$.result.places[0].name").value("310관"))
            .andExpect(jsonPath("$.result.places[0].visitRadiusMeters").value(75))
            .andExpect(jsonPath("$.result.places[1].courseOrder").value(2))
            .andExpect(jsonPath("$.result.places[1].name").value("도서관"))
            .andExpect(jsonPath("$.result.places[1].visitRadiusMeters").value(40))
            .andExpect(jsonPath("$.result.places[2].courseOrder").value(3))
            .andExpect(jsonPath("$.result.places[2].name").value("청룡연못"))
            .andExpect(jsonPath("$.result.places[2].visitRadiusMeters").value(30))
            .andExpect(jsonPath("$.result.places[1].placeId").value(library.id))
            .andExpect(jsonPath("$.result.places[1].latitude").value(37.5))
            .andExpect(jsonPath("$.result.places[1].longitude").value(126.95))
    }

    @Test
    fun readsThePlaceDescriptionAndVisitPointWithoutLocationInput() {
        mockMvc.perform(get("/api/places/${library.id}").header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("PLACE_200_2"))
            .andExpect(jsonPath("$.result.placeId").value(library.id))
            .andExpect(jsonPath("$.result.name").value("도서관"))
            .andExpect(jsonPath("$.result.description").value("테스트 장소 소개"))
            .andExpect(jsonPath("$.result.highlight").value("테스트 관람 포인트"))
            .andExpect(jsonPath("$.result.directions").value("테스트 찾아가는 길"))
            .andExpect(jsonPath("$.result.visitPointName").value("테스트 방문 확인 지점"))
            .andExpect(jsonPath("$.result.latitude").value(37.5))
            .andExpect(jsonPath("$.result.longitude").value(126.95))
            .andExpect(jsonPath("$.result.visitRadiusMeters").value(40))
    }

    @Test
    fun requiresServiceAuthenticationForAllPlaceOperations() {
        expectFailure(mockMvc.perform(get("/api/places")), 401, "COMMON_401_1")
        expectFailure(mockMvc.perform(get("/api/places/${library.id}")), 401, "COMMON_401_1")
        val visit = mockMvc.perform(
            post("/api/places/${library.id}/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(visitBody()),
        )
        expectFailure(visit, 401, "COMMON_401_1")
    }

    @Test
    fun requiresNicknameSetupForAllPlaceOperations() {
        val user = userRepository.save(User(kakaoUserId = 987654321L, createdAt = Instant.now()))
        val token = jwtService.issueAccessToken(requireNotNull(user.id))
        expectFailure(
            mockMvc.perform(get("/api/places").header(HttpHeaders.AUTHORIZATION, "Bearer $token")),
            403,
            "USER_403_1",
        )
        expectFailure(
            mockMvc.perform(get("/api/places/${library.id}").header(HttpHeaders.AUTHORIZATION, "Bearer $token")),
            403,
            "USER_403_1",
        )
        val visit = mockMvc.perform(
            post("/api/places/${library.id}/visits")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(visitBody()),
        )
        expectFailure(visit, 403, "USER_403_1")
    }

    @Test
    fun distinguishesMissingPlacesFromInvalidPlaceIdentifiers() {
        val missingId = Long.MAX_VALUE
        expectFailure(
            mockMvc.perform(get("/api/places/$missingId").header(HttpHeaders.AUTHORIZATION, bearer())),
            404,
            "PLACE_404_1",
        )
        expectFailure(requestVisit(visitBody(), missingId), 404, "PLACE_404_1")

        listOf("0", "-1", "abc", "1.5", "9223372036854775808").forEach { invalidId ->
            expectFailure(
                mockMvc.perform(get("/api/places/$invalidId").header(HttpHeaders.AUTHORIZATION, bearer())),
                400,
                "COMMON_400_2",
            )
            val visit = mockMvc.perform(
                post("/api/places/$invalidId/visits")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(visitBody()),
            )
            expectFailure(visit, 400, "COMMON_400_2")
        }
    }

    @Test
    fun checksCurrentLocationWithoutChangingUsersOrPlaces() {
        val originalUserCount = userRepository.count()
        val originalPlaceCount = placeRepository.count()
        repeat(2) {
            requestVisit(visitBody())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PLACE_200_3"))
                .andExpect(jsonPath("$.result.placeId").value(library.id))
                .andExpect(jsonPath("$.result.canWrite").value(true))
                .andExpect(jsonPath("$.result.reasonCode").isEmpty)
                .andExpect(jsonPath("$.result.distanceMeters").value(0.0))
                .andExpect(jsonPath("$.result.visitRadiusMeters").value(40))
        }
        assertEquals(originalUserCount, userRepository.count())
        assertEquals(originalPlaceCount, placeRepository.count())
        assertEquals("도서관", placeRepository.findById(requireNotNull(library.id)).orElseThrow().name)
    }

    @Test
    fun returnsEligibilityFailuresAsSuccessfulChecksWithAReasonCode() {
        listOf(
            visitBody(latitude = BigDecimal("37.501")) to "OUTSIDE_RADIUS",
            visitBody(accuracyMeters = BigDecimal("21")) to "LOW_ACCURACY",
            visitBody(measuredAt = Instant.now().minusSeconds(120).toString()) to "STALE_LOCATION",
        ).forEach { (body, reason) ->
            requestVisit(body)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PLACE_200_3"))
                .andExpect(jsonPath("$.result.canWrite").value(false))
                .andExpect(jsonPath("$.result.reasonCode").value(reason))
                .andExpect(jsonPath("$.result.visitRadiusMeters").value(40))
        }
    }

    @Test
    fun rejectsMalformedLocationBodiesAndInvalidCoordinates() {
        val values = mapOf<String, Any?>(
            "latitude" to BigDecimal("37.5"),
            "longitude" to BigDecimal("126.95"),
            "accuracyMeters" to BigDecimal("10"),
            "measuredAt" to Instant.now().toString(),
        )
        val missingFields = values.keys.map { field -> jsonMapper.writeValueAsString(values - field) }
        val invalidTypes = listOf(
            "latitude" to null,
            "latitude" to true,
            "latitude" to "NaN",
            "latitude" to "37.5",
            "longitude" to "126.95",
            "accuracyMeters" to "10",
            "accuracyMeters" to "",
            "accuracyMeters" to null,
            "accuracyMeters" to true,
            "measuredAt" to 1234,
            "measuredAt" to false,
        ).map { (field, value) ->
            jsonMapper.writeValueAsString(values + (field to value))
        }
        val invalidBodies = missingFields + invalidTypes + listOf(
            "{}",
            "null",
            "{",
            visitBody(latitude = BigDecimal("90.01")),
            visitBody(latitude = BigDecimal("-90.01")),
            visitBody(longitude = BigDecimal("180.01")),
            visitBody(longitude = BigDecimal("-180.01")),
            visitBody(accuracyMeters = BigDecimal("-1")),
            visitBody(measuredAt = "2026-11-07T05:00:00"),
            visitBody(measuredAt = "not-a-time"),
            visitBody(measuredAt = Instant.now().plusSeconds(120).toString()),
        )
        invalidBodies.forEach { body -> expectFailure(requestVisit(body), 400, "COMMON_400_2") }
    }

    private fun bearer(): String = "Bearer $accessToken"

    private fun requestVisit(body: String, placeId: Long = requireNotNull(library.id)): ResultActions = mockMvc.perform(
        post("/api/places/$placeId/visits")
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
    )

    private fun visitBody(
        latitude: BigDecimal = BigDecimal("37.5"),
        longitude: BigDecimal = BigDecimal("126.95"),
        accuracyMeters: BigDecimal = BigDecimal("10"),
        measuredAt: String = Instant.now().toString(),
    ): String = jsonMapper.writeValueAsString(
        mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracyMeters" to accuracyMeters,
            "measuredAt" to measuredAt,
        ),
    )

    private fun expectFailure(response: ResultActions, expectedStatus: Int, code: String) {
        response.andExpect(status().`is`(expectedStatus))
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.result").isEmpty)
    }

    private fun createPlace(order: Int, name: String, radius: Int): Place = Place(
        courseOrder = order,
        name = name,
        description = "테스트 장소 소개",
        highlight = "테스트 관람 포인트",
        directions = "테스트 찾아가는 길",
        visitPointName = "테스트 방문 확인 지점",
        latitude = BigDecimal("37.5"),
        longitude = BigDecimal("126.95"),
        visitRadiusMeters = radius,
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4.13")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_0900_ai_ci",
                "--default-time-zone=+00:00",
            )
    }
}
