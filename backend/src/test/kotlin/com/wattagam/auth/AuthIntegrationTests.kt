package com.wattagam.auth

import com.jayway.jsonpath.JsonPath
import com.wattagam.auth.kakao.KakaoClient
import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import com.wattagam.user.UserRepository
import com.wattagam.user.UserService
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEST_JWT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
private const val KAKAO_TOKEN = "kakao-access-token-for-integration-tests"
private const val KAKAO_USER_ID = 123456789L

@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=$TEST_JWT_SECRET",
        "app.auth.kakao.app-id=12345",
    ],
)
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthIntegrationTests.ProtectedTestController::class)
class AuthIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var kakaoClient: KakaoClient

    @MockitoSpyBean
    private lateinit var userService: UserService

    @BeforeEach
    fun prepareUserAndKakaoVerification() {
        userRepository.deleteAll()
        Mockito.`when`(kakaoClient.verifyAccessToken(KAKAO_TOKEN)).thenReturn(KAKAO_USER_ID)
    }

    @Test
    fun signInCreatesAndReusesTheVerifiedKakaoAccount() {
        val first = signIn()
        assertTrue(first.isNewUser)
        assertTrue(first.needsNickname)
        assertEquals(null, first.nickname)

        changeNickname(first.accessToken, "푸앙이123").andExpect(status().isOk)
        Mockito.`when`(kakaoClient.verifyAccessToken("another-kakao-token")).thenReturn(KAKAO_USER_ID)

        val repeated = signIn("another-kakao-token")
        assertFalse(repeated.isNewUser)
        assertFalse(repeated.needsNickname)
        assertEquals(first.userId, repeated.userId)
        assertEquals("푸앙이123", repeated.nickname)
        assertEquals(1L, userRepository.count())
        assertEquals(KAKAO_USER_ID, userRepository.findById(first.userId).orElseThrow().kakaoUserId)
    }

    @Test
    fun rejectsMissingOrBlankKakaoTokensBeforeExternalVerification() {
        listOf("{}", "{\"kakaoAccessToken\":\"\"}").forEach { body ->
            val request = mockMvc.perform(
                post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON).content(body),
            )
            expectFailure(request, 400, "COMMON_400_2")
        }
        Mockito.verifyNoInteractions(kakaoClient)
        assertEquals(0L, userRepository.count())
    }

    @Test
    fun publicKakaoLoginIgnoresExpiredOrMalformedServiceBearerTokens() {
        val login = signIn()
        val expired = signTestToken(login.userId, expiresAt = Instant.now().minusSeconds(600))
        listOf(expired, "malformed-service-token").forEach { token ->
            mockMvc.perform(
                post("/api/auth/kakao")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(mapOf("kakaoAccessToken" to KAKAO_TOKEN))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value("AUTH_200_1"))
                .andExpect(jsonPath("$.result.isNewUser").value(false))
        }
        Mockito.verify(kakaoClient, Mockito.times(3)).verifyAccessToken(KAKAO_TOKEN)
        assertEquals(1L, userRepository.count())
    }

    @Test
    fun rejectsInvalidKakaoCredentialsWithoutCreatingAUser() {
        Mockito.`when`(kakaoClient.verifyAccessToken(KAKAO_TOKEN))
            .thenThrow(ApiException(ErrorCode.KAKAO_UNAUTHORIZED))

        expectFailure(requestSignIn(KAKAO_TOKEN), 401, "AUTH_401_1")
        assertEquals(0L, userRepository.count())
    }

    @Test
    fun distinguishesKakaoUnavailabilityFromInvalidCredentials() {
        Mockito.`when`(kakaoClient.verifyAccessToken(KAKAO_TOKEN))
            .thenThrow(ApiException(ErrorCode.KAKAO_UNAVAILABLE))

        expectFailure(requestSignIn(KAKAO_TOKEN), 503, "AUTH_503_1")
        assertEquals(0L, userRepository.count())
    }

    @Test
    fun requiresServiceAuthenticationForProtectedOperations() {
        expectFailure(mockMvc.perform(get("/api/test/protected")), 401, "COMMON_401_1")
        expectFailure(mockMvc.perform(post("/api/auth/logout")), 401, "COMMON_401_1")
        val nicknameRequest = mockMvc.perform(
            patch("/api/me/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"푸앙이\"}"),
        )
        expectFailure(nicknameRequest, 401, "COMMON_401_1")
    }

    @Test
    fun readsOnboardingStateFromTheDatabaseWhenReusingTheSameJwt() {
        val login = signIn()
        expectFailure(requestProtected(login.accessToken), 403, "USER_403_1")

        changeNickname(login.accessToken, "푸앙이123")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("USER_200_1"))
            .andExpect(jsonPath("$.result.userId").value(login.userId))
            .andExpect(jsonPath("$.result.nickname").value("푸앙이123"))
            .andExpect(jsonPath("$.result.needsNickname").value(false))

        requestProtected(login.accessToken).andExpect(status().isOk)
        assertEquals("푸앙이123", userRepository.findById(login.userId).orElseThrow().nickname)
    }

    @Test
    fun validatesNicknameRulesAndAcceptsTheLengthBoundaries() {
        val login = signIn()
        listOf("가", "가".repeat(11), "닉 네임", "닉네임!", "").forEach { nickname ->
            expectFailure(changeNickname(login.accessToken, nickname), 400, "USER_400_1")
        }
        assertEquals(null, userRepository.findById(login.userId).orElseThrow().nickname)

        listOf("A1", "가".repeat(10)).forEach { nickname ->
            changeNickname(login.accessToken, nickname)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result.nickname").value(nickname))
        }
    }

    @Test
    fun rejectsNonStringNicknameValuesWithoutChangingTheUser() {
        val login = signIn()
        listOf("{\"nickname\":1234}", "{\"nickname\":true}", "{\"nickname\":null}").forEach { body ->
            val request = mockMvc.perform(
                patch("/api/me/nickname")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${login.accessToken}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
            expectFailure(request, 400, "COMMON_400_2")
        }
        assertEquals(null, userRepository.findById(login.userId).orElseThrow().nickname)
    }

    @Test
    fun rejectsTamperedAndExpiredServiceTokens() {
        val login = signIn()
        val parts = login.accessToken.split(".")
        val replacement = if (parts[2].startsWith("A")) {
            "B"
        } else {
            "A"
        }
        val tampered = "${parts[0]}.${parts[1]}.$replacement${parts[2].drop(1)}"
        expectFailure(requestProtected(tampered), 401, "COMMON_401_1")

        val expired = signTestToken(login.userId, expiresAt = Instant.now().minusSeconds(600))
        expectFailure(requestProtected(expired), 401, "COMMON_401_1")
    }

    @Test
    fun rejectsTokensForAnotherIssuerOrAudience() {
        val login = signIn()
        listOf(
            signTestToken(login.userId, issuer = "another-service"),
            signTestToken(login.userId, audience = "another-api"),
        ).forEach { token ->
            expectFailure(requestProtected(token), 401, "COMMON_401_1")
        }
    }

    @Test
    fun rejectsSignedServiceTokensWithoutAnExpirationClaim() {
        val login = signIn()

        expectFailure(requestProtected(signTestToken(login.userId, expiresAt = null)), 401, "COMMON_401_1")
    }

    @Test
    fun rejectsIssuedTokensWhenTheUserHasBeenDeleted() {
        val login = signIn()
        userRepository.deleteById(login.userId)

        expectFailure(requestProtected(login.accessToken), 401, "COMMON_401_1")
    }

    @Test
    fun returnsServerErrorWhenTheAuthenticationUserLookupDatabaseIsUnavailable() {
        val login = signIn()
        Mockito.doThrow(DataAccessResourceFailureException("테스트용 DB 연결 실패"))
            .`when`(userService).findByIdOrNull(login.userId)

        val response = requestProtected(login.accessToken)
        expectFailure(response, 500, "COMMON_500_1")
        response.andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."))
        assertTrue(userRepository.existsById(login.userId))
    }

    @Test
    fun logoutDoesNotRevokeAnAlreadyIssuedAccessToken() {
        val login = signIn()

        mockMvc.perform(
            post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer ${login.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("AUTH_200_2"))
            .andExpect(jsonPath("$.result").isEmpty)

        changeNickname(login.accessToken, "푸앙이").andExpect(status().isOk)
        requestProtected(login.accessToken).andExpect(status().isOk)
    }

    @Test
    fun concurrentLoginsCreateExactlyOneUser() {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..4).map {
                executor.submit<LoginResult> {
                    check(start.await(10, TimeUnit.SECONDS))
                    signIn()
                }
            }
            start.countDown()
            val logins = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertEquals(1L, userRepository.count())
            assertEquals(1, logins.map { it.userId }.toSet().size)
            assertEquals(1, logins.count { it.isNewUser })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun keepsHealthAvailableWithoutAServiceToken() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    private fun requestSignIn(kakaoAccessToken: String): ResultActions = mockMvc.perform(
        post("/api/auth/kakao")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(mapOf("kakaoAccessToken" to kakaoAccessToken))),
    )

    private fun signIn(kakaoAccessToken: String = KAKAO_TOKEN): LoginResult {
        val response = requestSignIn(kakaoAccessToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("AUTH_200_1"))
            .andExpect(jsonPath("$.result.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.result.expiresInSeconds").value(3600))
            .andReturn().response.contentAsString
        val timeStamp = JsonPath.read<String>(response, "$.timeStamp")
        assertTrue(timeStamp.endsWith("Z"))
        Instant.parse(timeStamp)
        return LoginResult(
            JsonPath.read<Number>(response, "$.result.user.userId").toLong(),
            JsonPath.read(response, "$.result.accessToken"),
            JsonPath.read(response, "$.result.isNewUser"),
            JsonPath.read(response, "$.result.needsNickname"),
            JsonPath.read(response, "$.result.user.nickname"),
        )
    }

    private fun changeNickname(accessToken: String, nickname: String): ResultActions = mockMvc.perform(
        patch("/api/me/nickname")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(mapOf("nickname" to nickname))),
    )

    private fun requestProtected(accessToken: String): ResultActions = mockMvc.perform(
        get("/api/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
    )

    private fun expectFailure(response: ResultActions, expectedStatus: Int, code: String) {
        response.andExpect(status().`is`(expectedStatus))
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.result").isEmpty)
    }

    private fun signTestToken(
        userId: Long,
        expiresAt: Instant? = Instant.now().plusSeconds(600),
        issuer: String = "wattagam",
        audience: String = "wattagam-api",
    ): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".toByteArray())
        val claims = mutableMapOf<String, Any>(
            "iss" to issuer,
            "sub" to userId.toString(),
            "aud" to listOf(audience),
            "iat" to Instant.now().minusSeconds(3600).epochSecond,
        )
        expiresAt?.let { claims["exp"] = it.epochSecond }
        val payload = encoder.encodeToString(jsonMapper.writeValueAsBytes(claims))
        val content = "$header.$payload"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(Base64.getDecoder().decode(TEST_JWT_SECRET), "HmacSHA256"))
        val signature = encoder.encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.US_ASCII)))
        return "$content.$signature"
    }

    private data class LoginResult(
        val userId: Long,
        val accessToken: String,
        val isNewUser: Boolean,
        val needsNickname: Boolean,
        val nickname: String?,
    )

    @RestController
    class ProtectedTestController {

        @GetMapping("/api/test/protected")
        fun getProtectedContent(): Map<String, Boolean> = mapOf("authenticated" to true)
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
