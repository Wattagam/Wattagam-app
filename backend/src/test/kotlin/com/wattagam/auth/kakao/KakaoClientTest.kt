package com.wattagam.auth.kakao

import com.sun.net.httpserver.HttpServer
import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.client.RestClient
import java.io.IOException
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KakaoClientTest {
    private lateinit var server: HttpServer
    private val response = AtomicReference(MockResponse(200, validResponse()))
    private val requests = ConcurrentLinkedQueue<RecordedRequest>()

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/user/access_token_info") { exchange ->
            requests.add(
                RecordedRequest(
                    exchange.requestMethod,
                    exchange.requestURI.toString(),
                    exchange.requestHeaders.getFirst("Authorization"),
                ),
            )
            val currentResponse = response.get()
            try {
                if (currentResponse.delayMillis > 0) {
                    Thread.sleep(currentResponse.delayMillis)
                }
                val bytes = currentResponse.body.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json")
                for ((name, value) in currentResponse.headers) {
                    exchange.responseHeaders.set(name, value)
                }
                exchange.sendResponseHeaders(currentResponse.status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: IOException) {
                // 제한 시간 검증에서는 클라이언트가 먼저 연결을 닫는다.
            } finally {
                exchange.close()
            }
        }
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun verifiesTheKakaoUserAndSendsTheTokenOnlyInTheBearerHeader() {
        response.set(MockResponse(200, """{"id":123,"app_id":12345,"expires_in":60,"extra":"ignored"}"""))

        assertEquals(123L, createClient().verifyAccessToken(TOKEN))
        assertEquals(
            RecordedRequest("GET", "/v1/user/access_token_info", "Bearer " + TOKEN),
            requests.single(),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["", " ", " sdk-token", "sdk-token ", "sdk token", "Bearer sdk-token", "bearer sdk-token", "Bearer"],
    )
    fun rejectsInvalidTokenInputWithoutCallingKakao(token: String) {
        assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken(token) }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun rejectsControlCharactersWithoutCallingKakao() {
        assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken("sdk\u0000token") }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun rejectsATokenFromAnotherKakaoApp() {
        response.set(MockResponse(200, validResponse(appId = 54321)))

        assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken(TOKEN) }
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1])
    fun rejectsExpiredTokens(expiresIn: Long) {
        response.set(MockResponse(200, validResponse(expiresIn = expiresIn)))

        assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken(TOKEN) }
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1])
    fun rejectsInvalidUserIds(userId: Long) {
        response.set(MockResponse(200, validResponse(id = userId)))

        assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken(TOKEN) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{}",
            """{"app_id":12345,"expires_in":60}""",
            """{"id":123,"expires_in":60}""",
            """{"id":123,"app_id":12345}""",
            "null",
        ],
    )
    fun rejectsIncompleteTokenInformation(body: String) {
        response.set(MockResponse(200, body))

        assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken(TOKEN) }
    }

    @Test
    fun mapsProviderUnauthorizedWithoutExposingTheProviderBody() {
        response.set(MockResponse(401, """{"msg":"PROVIDER_PRIVATE_DETAIL","code":-401}"""))

        val exception = assertFailure(ErrorCode.KAKAO_UNAUTHORIZED) { createClient().verifyAccessToken(TOKEN) }
        assertFalse(exception.message.orEmpty().contains("PROVIDER_PRIVATE_DETAIL"))
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 403, 429, 500, 503])
    fun distinguishesProviderFailuresFromInvalidUserTokens(status: Int) {
        response.set(MockResponse(status, """{"msg":"PROVIDER_PRIVATE_DETAIL"}"""))

        assertFailure(ErrorCode.KAKAO_UNAVAILABLE) { createClient().verifyAccessToken(TOKEN) }
    }

    @Test
    fun rejectsRedirectsWithoutForwardingTheToken() {
        response.set(
            MockResponse(
                302,
                validResponse(),
                headers = mapOf("Location" to "/v1/user/access_token_info?redirected=true"),
            ),
        )

        assertFailure(ErrorCode.KAKAO_UNAVAILABLE) { createClient().verifyAccessToken(TOKEN) }
        assertEquals(1, requests.size)
    }

    @Test
    fun mapsMalformedJsonToProviderUnavailability() {
        response.set(MockResponse(200, """{"id":"""))

        assertFailure(ErrorCode.KAKAO_UNAVAILABLE) { createClient().verifyAccessToken(TOKEN) }
    }

    @Test
    fun mapsAReadTimeoutToProviderUnavailability() {
        response.set(MockResponse(200, validResponse(), delayMillis = 500))

        assertFailure(ErrorCode.KAKAO_UNAVAILABLE) {
            createClient(readTimeout = Duration.ofMillis(100)).verifyAccessToken(TOKEN)
        }
    }

    private fun createClient(readTimeout: Duration = Duration.ofSeconds(1)): KakaoClient {
        val properties = KakaoProperties(
            appId = APP_ID,
            baseUrl = "http://127.0.0.1:" + server.address.port,
            connectTimeout = Duration.ofSeconds(1),
            readTimeout = readTimeout,
        )
        return KakaoClient(properties, RestClient.builder())
    }

    private fun assertFailure(errorCode: ErrorCode, action: () -> Unit): ApiException {
        val exception = assertFailsWith<ApiException>(block = action)
        assertEquals(errorCode, exception.errorCode)
        assertNull(exception.cause)
        assertFalse(exception.message.orEmpty().contains(TOKEN))
        return exception
    }

    private data class MockResponse(
        val status: Int,
        val body: String,
        val delayMillis: Long = 0,
        val headers: Map<String, String> = emptyMap(),
    )

    private data class RecordedRequest(val method: String, val uri: String, val authorization: String?)

    companion object {
        private const val APP_ID = 12345L
        private const val TOKEN = "sdk-token"

        private fun validResponse(id: Long = 123, appId: Long = APP_ID, expiresIn: Long = 60): String {
            return """{"id":$id,"app_id":$appId,"expires_in":$expiresIn}"""
        }
    }
}
