package com.wattagam.app.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 서버가 실패 응답을 보냈을 때. [code] 는 'PLACE_404_1' 같은 서버 오류 코드, [message] 는 서버가 준 사용자용 문구.
 * 네트워크에 닿지 못한 실패는 이 예외가 아니라 IOException 으로 올라온다.
 */
class ApiException(val status: Int, val code: String?, override val message: String?) : Exception(message)

/**
 * 왔다감 서버 API. 요청 하나가 함수 하나다. 토큰 갱신·재시도는 [ServerRepository] 가 맡는다.
 * 인증 헤더와 위치 값은 로그에 남기지 않는다 (이 클래스는 로그를 쓰지 않는다).
 */
class WattagamApi(baseUrl: String, private val client: OkHttpClient, private val accessToken: () -> String?) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    suspend fun loginWithKakao(kakaoAccessToken: String): LoginDto =
        call(post("api/auth/kakao", KakaoLoginBody(kakaoAccessToken), KakaoLoginBody.serializer(), authorized = false), LoginDto.serializer())

    suspend fun logout() {
        call(post("api/auth/logout", null, Unit.serializer()), Unit.serializer(), resultRequired = false)
    }

    suspend fun updateNickname(nickname: String): NicknameDto =
        call(request("api/me/nickname").patch(jsonBody(NicknameBody(nickname), NicknameBody.serializer())).build(), NicknameDto.serializer())

    suspend fun withdraw() {
        call(request("api/me").delete().build(), Unit.serializer(), resultRequired = false)
    }

    suspend fun places(): PlaceListDto = call(request("api/places").build(), PlaceListDto.serializer())

    suspend fun place(placeId: Long): PlaceDetailDto = call(request("api/places/$placeId").build(), PlaceDetailDto.serializer())

    suspend fun visit(placeId: Long, location: LocationBody): VisitDto =
        call(post("api/places/$placeId/visits", location, LocationBody.serializer()), VisitDto.serializer())

    suspend fun stamps(): StampBoardDto = call(request("api/me/stamps").build(), StampBoardDto.serializer())

    /** 방명록 등록 (사진 포함). 같은 [clientRequestId] 로 다시 보내면 서버가 처음 결과를 돌려준다 */
    suspend fun createGuestbook(placeId: Long, image: File, message: String, location: LocationBody, clientRequestId: String): GuestbookCreatedDto {
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", image.name, image.asRequestBody(JPEG))
            .apply { if (message.isNotEmpty()) addFormDataPart("message", message) }
            .addFormDataPart("latitude", location.latitude.toString())
            .addFormDataPart("longitude", location.longitude.toString())
            .addFormDataPart("accuracyMeters", location.accuracyMeters.toString())
            .addFormDataPart("measuredAt", location.measuredAt)
            .addFormDataPart("clientRequestId", clientRequestId)
            .build()
        return call(request("api/places/$placeId/guestbook").post(form).build(), GuestbookCreatedDto.serializer())
    }

    suspend fun gallery(placeId: Long, page: Int, size: Int): GuestbookPageDto =
        call(request("api/places/$placeId/guestbook", "page" to page, "size" to size).build(), GuestbookPageDto.serializer())

    suspend fun guestbook(entryId: Long): GuestbookDetailDto = call(request("api/guestbook/$entryId").build(), GuestbookDetailDto.serializer())

    suspend fun myGuestbooks(page: Int, size: Int): GuestbookPageDto =
        call(request("api/me/guestbook", "page" to page, "size" to size).build(), GuestbookPageDto.serializer())

    private fun request(path: String, vararg query: Pair<String, Int>, authorized: Boolean = true): Request.Builder {
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k, v) -> addQueryParameter(k, v.toString()) } }.build()
        return Request.Builder().url(url).apply {
            if (authorized) accessToken()?.let { header("Authorization", "Bearer $it") }
        }
    }

    private fun <B> post(path: String, body: B?, serializer: KSerializer<B>, authorized: Boolean = true): Request =
        request(path, authorized = authorized).post(if (body == null) EMPTY else jsonBody(body, serializer)).build()

    private fun <B> jsonBody(body: B, serializer: KSerializer<B>): RequestBody = json.encodeToString(serializer, body).toRequestBody(JSON)

    private suspend fun <T> call(request: Request, serializer: KSerializer<T>, resultRequired: Boolean = true): T {
        val (status, body) = execute(request)
        val envelope = runCatching { json.decodeFromString(Envelope.serializer(serializer), body) }.getOrNull()
        if (status !in 200..299) throw ApiException(status, envelope?.code, envelope?.message)
        @Suppress("UNCHECKED_CAST")
        return envelope?.result ?: if (resultRequired) throw ApiException(status, envelope?.code, null) else Unit as T
    }

    private suspend fun execute(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        val call = client.newCall(request)
        // 화면을 떠나 코루틴이 취소되면 진행 중인 요청도 끊는다
        val cancelHandle = coroutineContext[Job]?.invokeOnCompletion { if (it is CancellationException) call.cancel() }
        try {
            call.execute().use { it.code to it.body.string() }
        } finally {
            cancelHandle?.dispose()
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val JSON = "application/json".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
        val EMPTY = ByteArray(0).toRequestBody(null)
    }
}
