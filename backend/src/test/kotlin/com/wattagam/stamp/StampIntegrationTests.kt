package com.wattagam.stamp

import com.wattagam.auth.JwtService
import com.wattagam.user.User
import com.wattagam.user.UserRepository
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.auth.kakao.app-id=12345",
    ],
)
@AutoConfigureMockMvc
@Testcontainers
class StampIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var stampService: StampService

    @Autowired
    private lateinit var stampRepository: StampRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    fun clearTestUsers() {
        userRepository.deleteAll()
    }

    @Test
    fun listsEveryPlaceInCourseOrderOnAnEmptyStampBoard() {
        val userId = createUser(101L)
        val result = requestForUser("/api/me/stamps", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("STAMP_200_1"))
            .andExpect(jsonPath("$.result.earnedCount").value(0))
            .andExpect(jsonPath("$.result.totalCount").value(3))
            .andExpect(jsonPath("$.result.stamps.length()").value(3))

        listOf("310관", "도서관", "청룡연못").forEachIndexed { index, placeName ->
            val slot = "$.result.stamps[$index]"
            result.andExpect(jsonPath("$slot.placeId").value(index + 1))
                .andExpect(jsonPath("$slot.courseOrder").value(index + 1))
                .andExpect(jsonPath("$slot.placeName").value(placeName))
                .andExpect(jsonPath("$slot.earned").value(false))
                .andExpect(jsonPath("$slot.earnedAt").value(nullValue()))
                .andExpect(jsonPath("$slot.firstEntryId").value(nullValue()))
        }
    }

    @Test
    fun returnsOnlyTheCurrentUsersStampsOnBothViews() {
        val userId = createUser(101L)
        val otherUserId = createUser(102L)
        grantStamp(userId, 2L, 201L)
        grantStamp(otherUserId, 3L, 202L)
        val earnedStamp = stampRepository.findAllByUserId(userId).single()

        requestForUser("/api/me/stamps", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.earnedCount").value(1))
            .andExpect(jsonPath("$.result.totalCount").value(3))
            .andExpect(jsonPath("$.result.stamps[0].earned").value(false))
            .andExpect(jsonPath("$.result.stamps[1].earned").value(true))
            .andExpect(jsonPath("$.result.stamps[1].earnedAt").value(earnedStamp.earnedAt.toString()))
            .andExpect(jsonPath("$.result.stamps[1].firstEntryId").value(201))
            .andExpect(jsonPath("$.result.stamps[2].earned").value(false))
            .andExpect(jsonPath("$.result.stamps[2].firstEntryId").value(nullValue()))

        requestForUser("/api/places", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.places[0].stampEarned").value(false))
            .andExpect(jsonPath("$.result.places[1].stampEarned").value(true))
            .andExpect(jsonPath("$.result.places[2].stampEarned").value(false))
    }

    @Test
    fun requiresAuthenticationAndNicknameSetupForTheStampBoard() {
        mockMvc.perform(get("/api/me/stamps"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("COMMON_401_1"))

        requestForUser("/api/me/stamps", createUser(101L, nickname = null))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("USER_403_1"))
    }

    @Test
    fun keepsTheFirstStampAndCommitsLaterWorkAfterADuplicateGrant() {
        val userId = createUser(101L)
        grantStamp(userId, 1L, 201L)
        val firstStamp = stampRepository.findAllByUserId(userId).single()

        TransactionTemplate(transactionManager).executeWithoutResult {
            stampService.grantForPublishedEntry(userId, 1L, 202L)
            stampService.grantForPublishedEntry(userId, 2L, 203L)
        }

        val stamps = stampRepository.findAllByUserId(userId).associateBy { it.placeId }
        val original = stamps.getValue(1L)
        assertEquals(setOf(1L, 2L), stamps.keys)
        assertEquals(firstStamp.id, original.id)
        assertEquals(201L, original.firstEntryId)
        assertEquals(firstStamp.earnedAt, original.earnedAt)
        assertEquals(203L, stamps.getValue(2L).firstEntryId)
    }

    @Test
    fun commitsExactlyOneStampWhenPublicationsRaceForTheSamePlace() {
        val userId = createUser(101L)
        val entryIds = listOf(201L, 202L)
        val ready = CountDownLatch(entryIds.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(entryIds.size)
        try {
            val publications = entryIds.map { entryId ->
                executor.submit {
                    TransactionTemplate(transactionManager).executeWithoutResult {
                        ready.countDown()
                        check(start.await(10, TimeUnit.SECONDS))
                        stampService.grantForPublishedEntry(userId, 1L, entryId)
                    }
                }
            }
            check(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            publications.forEach { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        val stamp = stampRepository.findAllByUserId(userId).single()
        assertTrue(stamp.firstEntryId in entryIds)
    }

    @Test
    fun rollsBackTheStampWhenPublicationRollsBack() {
        val userId = createUser(101L)
        TransactionTemplate(transactionManager).executeWithoutResult { transaction ->
            stampService.grantForPublishedEntry(userId, 1L, 201L)
            assertEquals(201L, stampRepository.findAllByUserId(userId).single().firstEntryId)
            transaction.setRollbackOnly()
        }

        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun rejectsGrantsOutsideAPublicationTransaction() {
        val userId = createUser(101L)
        assertFailsWith<IllegalTransactionStateException> {
            stampService.grantForPublishedEntry(userId, 1L, 201L)
        }
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun deletesOnlyTheRemovedUsersStamps() {
        val userId = createUser(101L)
        val otherUserId = createUser(102L)
        grantStamp(userId, 1L, 201L)
        grantStamp(otherUserId, 1L, 202L)

        userRepository.deleteById(userId)

        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        assertEquals(202L, stampRepository.findAllByUserId(otherUserId).single().firstEntryId)
    }

    private fun createUser(kakaoUserId: Long, nickname: String? = "푸앙이"): Long {
        val user = userRepository.save(User(kakaoUserId = kakaoUserId, nickname = nickname, createdAt = Instant.now()))
        return requireNotNull(user.id)
    }

    private fun requestForUser(path: String, userId: Long): ResultActions = mockMvc.perform(
        get(path).header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtService.issueAccessToken(userId)}"),
    )

    private fun grantStamp(userId: Long, placeId: Long, entryId: Long) {
        TransactionTemplate(transactionManager).executeWithoutResult {
            stampService.grantForPublishedEntry(userId, placeId, entryId)
        }
    }

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
