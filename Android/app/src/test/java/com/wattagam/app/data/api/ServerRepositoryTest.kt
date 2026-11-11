package com.wattagam.app.data.api

import com.wattagam.app.data.AccountProblem
import com.wattagam.app.data.PublishOutcome
import com.wattagam.app.data.auth.Session
import com.wattagam.app.data.auth.SessionStorage
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.location.VisitEligibility
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** 서버 응답을 흉내 내는 가짜 연결로 요청 형식·응답 해석·토큰 재발급을 확인한다 */
class ServerRepositoryTest {
    private class MemorySession(var session: Session? = null) : SessionStorage {
        override fun load() = session
        override fun save(session: Session) { this.session = session }
        override fun clear() { session = null }
    }

    private val requests = mutableListOf<Request>()
    private var respond: (Request) -> Pair<Int, String> = { 500 to "" }
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        requests += request
        val (code, body) = respond(request)
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("test")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()

    private val session = MemorySession()
    private var kakaoToken: String? = "kakao-token"
    private val now = 1_000_000L
    private val repository = ServerRepository("http://test.local/", client, session, { kakaoToken }, "캠퍼스 산책", now = { now })

    private fun ok(result: String) = 200 to """{"isSuccess":true,"status":200,"code":"OK","message":"ok","result":$result}"""
    private fun error(status: Int, code: String, message: String) =
        status to """{"isSuccess":false,"status":$status,"code":"$code","message":"$message","result":null}"""
    private fun login(token: String, nickname: String?) =
        ok("""{"accessToken":"$token","tokenType":"Bearer","expiresInSeconds":3600,"isNewUser":false,"needsNickname":${nickname == null},"user":{"userId":7,"nickname":${nickname?.let { "\"$it\"" } ?: "null"}}}""")

    private val placesJson =
        """{"places":[{"placeId":2,"courseOrder":2,"name":"도서관","latitude":37.504779,"longitude":126.957959,"visitRadiusMeters":40,"stampEarned":false},""" +
            """{"placeId":1,"courseOrder":1,"name":"310관","latitude":37.503645,"longitude":126.956032,"visitRadiusMeters":75,"stampEarned":true}]}"""

    private fun loggedIn(token: String = "old") {
        session.session = Session(token, now + 3_600_000, "하늘")
    }

    private fun Request.bodyText() = Buffer().also { body!!.writeTo(it) }.readUtf8()

    @Test fun signInSavesServerTokenAndAsksForNicknameWhenMissing() = runBlocking {
        respond = { login("jwt", nickname = null) }
        val account = repository.signIn("kakao-token")
        assertNull(account.nickname)
        assertEquals("jwt", session.session?.accessToken)
        assertEquals(now + 3_600_000, session.session?.expiresAtMillis)
        assertEquals("/api/auth/kakao", requests.single().url.encodedPath)
        assertEquals("""{"kakaoAccessToken":"kakao-token"}""", requests.single().bodyText())
        assertNull("로그인 요청에는 인증 헤더가 없다", requests.single().header("Authorization"))
    }

    @Test fun restoreSessionUsesSavedTokenWithoutNetwork() = runBlocking {
        loggedIn()
        assertEquals("하늘", repository.restoreSession()?.nickname)
        assertTrue(requests.isEmpty())
    }

    @Test fun placesAreSortedByCourseOrderWithBearerToken() = runBlocking {
        loggedIn("jwt")
        respond = { ok(placesJson) }
        val places = repository.places()
        assertEquals(listOf("1", "2"), places.map { it.id })
        assertTrue(places.first().stamped)
        assertEquals("Bearer jwt", requests.single().header("Authorization"))
        assertEquals("310관", repository.cachedPlace("1")?.name)
    }

    @Test fun expiredTokenIsExchangedOnceAndRequestRetried() = runBlocking {
        loggedIn("old")
        respond = { request ->
            when {
                request.url.encodedPath == "/api/auth/kakao" -> login("new", "하늘")
                request.header("Authorization") == "Bearer old" -> error(401, "COMMON_401_1", "인증 정보가 없거나 유효하지 않습니다.")
                else -> ok(placesJson)
            }
        }
        assertEquals(2, repository.places().size)
        assertEquals(listOf("/api/places", "/api/auth/kakao", "/api/places"), requests.map { it.url.encodedPath })
        assertEquals("new", session.session?.accessToken)
    }

    @Test fun sessionExpiresWhenKakaoLoginIsGone() = runBlocking {
        loggedIn("old")
        kakaoToken = null
        respond = { error(401, "COMMON_401_1", "인증 정보가 없거나 유효하지 않습니다.") }
        try {
            repository.places()
            fail("다시 로그인해야 한다")
        } catch (_: AccountProblem.SessionExpired) {
        }
        assertNull(session.session)
    }

    @Test fun nicknameRequiredClearsSavedNickname() = runBlocking {
        loggedIn()
        respond = { error(403, "USER_403_1", "닉네임 설정이 필요합니다.") }
        try {
            repository.stampBoard()
            fail("닉네임을 먼저 정해야 한다")
        } catch (_: AccountProblem.NicknameRequired) {
        }
        assertNull(session.session?.nickname)
        assertNull(repository.restoreSession()?.nickname)
    }

    @Test fun serverErrorKeepsCodeAndMessage() = runBlocking {
        loggedIn()
        respond = { error(400, "USER_400_1", "닉네임은 한글, 영문, 숫자로 2자 이상 10자 이하로 입력해 주세요.") }
        try {
            repository.saveNickname("!")
            fail()
        } catch (e: ApiException) {
            assertEquals("USER_400_1", e.code)
            assertEquals("닉네임은 한글, 영문, 숫자로 2자 이상 10자 이하로 입력해 주세요.", e.message)
        }
    }

    @Test fun visitSendsCurrentLocationAndMapsReasons() = runBlocking {
        loggedIn()
        val fix = LocationFix.Found(37.5036, 126.956, 8.3f, measuredAtMillis = 1_762_415_995_000)
        respond = { ok("""{"placeId":1,"canWrite":false,"reasonCode":"OUTSIDE_RADIUS","distanceMeters":120.5,"visitRadiusMeters":75}""") }
        assertEquals(VisitEligibility.Outside, repository.checkVisit("1", fix))
        assertEquals("/api/places/1/visits", requests.last().url.encodedPath)
        assertEquals(
            """{"latitude":37.5036,"longitude":126.956,"accuracyMeters":8.3,"measuredAt":"2025-11-06T07:59:55Z"}""",
            requests.last().bodyText(),
        )

        respond = { ok("""{"placeId":1,"canWrite":false,"reasonCode":"STALE_LOCATION","distanceMeters":null,"visitRadiusMeters":75}""") }
        assertEquals(VisitEligibility.Unavailable, repository.checkVisit("1", fix))
        respond = { ok("""{"placeId":1,"canWrite":true,"reasonCode":null,"distanceMeters":3.2,"visitRadiusMeters":75}""") }
        assertEquals(VisitEligibility.Allowed, repository.checkVisit("1", fix))
    }

    @Test fun publishSendsMultipartWithRequestKeyAndReturnsStamp() = runBlocking {
        loggedIn()
        val photo = File.createTempFile("upload", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)); deleteOnExit() }
        respond = {
            201 to """{"isSuccess":true,"status":201,"code":"GUESTBOOK_201_1","message":"방명록을 등록했습니다.","result":""" +
                """{"entryId":101,"placeId":1,"imageUrl":"https://img/101.jpg","message":"왔다감","createdAt":"2026-11-06T08:00:00Z",""" +
                """"stampAwarded":true,"earnedCount":1,"totalCount":3}}"""
        }
        val outcome = repository.publish("1", photo, "왔다감", LocationFix.Found(37.5, 126.9, 5f, 0L), "req-1")
        val published = outcome as PublishOutcome.Published
        assertEquals("101", published.entryId)
        assertTrue(published.stampAwarded)
        assertEquals(3, published.totalCount)
        assertEquals("2026. 11. 6. 17:00", published.createdAt)

        val request = requests.single()
        assertEquals("/api/places/1/guestbook", request.url.encodedPath)
        assertTrue(request.body!!.contentType().toString().startsWith("multipart/form-data"))
        val body = request.bodyText()
        for (part in listOf("image", "message", "latitude", "longitude", "accuracyMeters", "measuredAt", "clientRequestId")) {
            assertTrue("$part 가 있어야 한다", body.contains("name=\"$part\""))
        }
        assertTrue(body.contains("req-1"))
    }

    @Test fun publishWithoutMessageOmitsMessagePart() = runBlocking {
        loggedIn()
        val photo = File.createTempFile("upload", ".jpg").apply { writeBytes(byteArrayOf(1)); deleteOnExit() }
        respond = { error(403, "GUESTBOOK_403_1", "방문 반경 밖에서는 방명록을 작성할 수 없습니다.") }
        try {
            repository.publish("1", photo, "", LocationFix.Found(37.5, 126.9, 5f, 0L), "req-2")
            fail()
        } catch (e: ApiException) {
            assertEquals(VisitEligibility.Outside, e.locationRejection())
        }
        assertFalse(requests.single().bodyText().contains("name=\"message\""))
    }

    @Test fun galleryPagesCarryPlaceNameAndDisplayTime() = runBlocking {
        loggedIn()
        respond = { request ->
            if (request.url.encodedPath == "/api/places") ok(placesJson)
            else ok(
                """{"entries":[{"entryId":101,"nickname":"푸앙이123","imageUrl":"https://img/101.jpg","message":null,""" +
                    """"createdAt":"2026-11-06T15:30:00Z","edited":false,"mine":true}],"page":1,"size":20,"hasNext":false}""",
            )
        }
        val page = repository.gallery("2", page = 1)
        val entry = page.entries.single()
        assertEquals("도서관", entry.placeName)
        assertEquals("푸앙이123", entry.author)
        assertEquals("", entry.message)
        assertEquals("2026. 11. 7. 00:30", entry.createdAt)
        assertFalse(page.hasNext)
        val galleryRequest = requests.last().url
        assertEquals("1", galleryRequest.queryParameter("page"))
        assertEquals("20", galleryRequest.queryParameter("size"))
    }

    @Test fun badIdIsNotFoundWithoutRequest() = runBlocking {
        loggedIn()
        try {
            repository.guestbook("g1")
            fail()
        } catch (e: ApiException) {
            assertEquals("GUESTBOOK_404_1", e.code)
        }
        assertTrue(requests.isEmpty())
    }
}
