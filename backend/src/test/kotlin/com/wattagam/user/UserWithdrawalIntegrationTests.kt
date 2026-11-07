package com.wattagam.user

import com.wattagam.auth.JwtService
import com.wattagam.auth.kakao.KakaoClient
import com.wattagam.common.api.ApiException
import com.wattagam.stamp.StampRepository
import com.wattagam.stamp.StampService
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.auth.kakao.app-id=12345",
    ],
)
@AutoConfigureMockMvc
@Testcontainers
class UserWithdrawalIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stampRepository: StampRepository

    @Autowired
    private lateinit var stampService: StampService

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean
    private lateinit var contentCleanup: WithdrawalContentCleanup

    @MockitoBean
    private lateinit var kakaoClient: KakaoClient

    @MockitoSpyBean
    private lateinit var userService: UserService

    @BeforeEach
    fun clearTestUsers() {
        userRepository.deleteAll()
    }

    @Test
    fun deletesTheAuthenticatedUserAndTheirStampsAndRejectsTheirOldToken() {
        val userId = createUser(101L)
        val otherUserId = createUser(102L)
        grantStamp(userId, 201L)
        grantStamp(otherUserId, 202L)
        val token = jwtService.issueAccessToken(userId)
        Mockito.`when`(contentCleanup.deleteForUser(userId)).thenReturn(true)

        expectSuccessfulWithdrawal(requestWithdrawal(token))

        assertFalse(userRepository.existsById(userId))
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        assertTrue(userRepository.existsById(otherUserId))
        assertEquals(202L, stampRepository.findAllByUserId(otherUserId).single().firstEntryId)
        requestStampBoard(token)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("COMMON_401_1"))
        Mockito.verify(contentCleanup).deleteForUser(userId)
        Mockito.verifyNoMoreInteractions(contentCleanup)
    }

    @Test
    fun allowsWithdrawalBeforeOnboardingAndCreatesANewAccountOnTheNextKakaoLogin() {
        val kakaoUserId = 101L
        val oldUserId = createUser(kakaoUserId, nickname = null)
        Mockito.`when`(contentCleanup.deleteForUser(oldUserId)).thenReturn(true)
        Mockito.`when`(kakaoClient.verifyAccessToken("kakao-token-for-withdrawal-test")).thenReturn(kakaoUserId)

        expectSuccessfulWithdrawal(requestWithdrawal(jwtService.issueAccessToken(oldUserId)))
        assertNull(userRepository.findByKakaoUserId(kakaoUserId))

        mockMvc.perform(
            post("/api/auth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"kakaoAccessToken":"kakao-token-for-withdrawal-test"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.isNewUser").value(true))
            .andExpect(jsonPath("$.result.needsNickname").value(true))
            .andExpect(jsonPath("$.result.user.nickname").value(nullValue()))
        val newUserId = requireNotNull(userRepository.findByKakaoUserId(kakaoUserId)?.id)
        assertNotEquals(oldUserId, newUserId)
        assertTrue(stampRepository.findAllByUserId(newUserId).isEmpty())
    }

    @Test
    fun preservesTheAccountAndStampsWhenContentCleanupNeedsARetry() {
        val userId = createUser(101L)
        grantStamp(userId, 201L)
        val token = jwtService.issueAccessToken(userId)
        Mockito.doAnswer {
            // 정리 단계의 DB 삭제도 실패 응답과 함께 롤백되는지 확인한다.
            stampRepository.deleteAll(stampRepository.findAllByUserId(userId))
            stampRepository.flush()
            false
        }.doReturn(true).`when`(contentCleanup).deleteForUser(userId)

        requestWithdrawal(token)
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("USER_503_1"))

        assertTrue(userRepository.existsById(userId))
        requestStampBoard(token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.earnedCount").value(1))
        expectSuccessfulWithdrawal(requestWithdrawal(token))
        assertFalse(userRepository.existsById(userId))
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        Mockito.verify(contentCleanup, Mockito.times(2)).deleteForUser(userId)
    }

    @Test
    fun completesOnlyOneWithdrawalWhenTwoAuthenticatedRequestsRace() {
        val userId = createUser(101L)
        val token = jwtService.issueAccessToken(userId)
        val authenticated = CountDownLatch(2)
        Mockito.doAnswer { invocation ->
            val user = invocation.callRealMethod()
            authenticated.countDown()
            check(authenticated.await(10, TimeUnit.SECONDS))
            user
        }.`when`(userService).findByIdOrNull(userId)
        Mockito.`when`(contentCleanup.deleteForUser(userId)).thenReturn(true)

        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = List(2) {
                executor.submit<Int> { requestWithdrawal(token).andReturn().response.status }
            }
            assertEquals(listOf(200, 401), requests.map { it.get(20, TimeUnit.SECONDS) }.sorted())
        } finally {
            executor.shutdownNow()
        }
        assertFalse(userRepository.existsById(userId))
        Mockito.verify(contentCleanup).deleteForUser(userId)
    }

    @Test
    fun rejectsWithdrawalWithoutServiceAuthentication() {
        mockMvc.perform(delete("/api/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("COMMON_401_1"))
        Mockito.verifyNoInteractions(contentCleanup)
    }

    @Test
    fun preservesTheAccountWhenNoContentCleanupAdapterIsAvailable() {
        val userId = createUser(101L)
        val provider = DefaultListableBeanFactory().getBeanProvider(WithdrawalContentCleanup::class.java)
        val service = UserWithdrawalService(userRepository, provider, transactionManager)

        val exception = assertFailsWith<ApiException> { service.withdraw(userId) }

        assertEquals("USER_503_1", exception.errorCode.code)
        assertTrue(userRepository.existsById(userId))
    }

    private fun createUser(kakaoUserId: Long, nickname: String? = "푸앙이"): Long {
        val user = userRepository.save(User(kakaoUserId = kakaoUserId, nickname = nickname, createdAt = Instant.now()))
        return requireNotNull(user.id)
    }

    private fun grantStamp(userId: Long, entryId: Long) {
        TransactionTemplate(transactionManager).executeWithoutResult {
            stampService.grantForPublishedEntry(userId, 1L, entryId)
        }
    }

    private fun requestWithdrawal(token: String): ResultActions =
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))

    private fun requestStampBoard(token: String): ResultActions =
        mockMvc.perform(get("/api/me/stamps").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))

    private fun expectSuccessfulWithdrawal(response: ResultActions) {
        response.andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("USER_200_2"))
            .andExpect(jsonPath("$.result").value(nullValue()))
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
