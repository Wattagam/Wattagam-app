package com.wattagam.app.data.api

import com.wattagam.app.data.Account
import com.wattagam.app.data.AccountProblem
import com.wattagam.app.data.CampusPlace
import com.wattagam.app.data.CampusPlaceDetail
import com.wattagam.app.data.GuestbookEntry
import com.wattagam.app.data.GuestbookPage
import com.wattagam.app.data.PublishOutcome
import com.wattagam.app.data.StampBoardData
import com.wattagam.app.data.StampSlot
import com.wattagam.app.data.WattagamRepository
import com.wattagam.app.data.auth.Session
import com.wattagam.app.data.auth.SessionStorage
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.location.VisitEligibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round

/**
 * 왔다감 서버에 연결한 저장소.
 * - 서비스 토큰이 만료돼 401 이 오면 카카오 토큰으로 한 번 다시 로그인하고 같은 요청을 다시 보낸다.
 *   그래도 안 되면 [AccountProblem.SessionExpired] 로 로그인 화면에 보낸다.
 * - 닉네임이 없는 계정이 막히면(USER_403_1) [AccountProblem.NicknameRequired].
 * - 작성 가능 여부·도장은 서버 응답만 믿는다.
 */
class ServerRepository(
    baseUrl: String,
    client: OkHttpClient,
    private val session: SessionStorage,
    /** 저장된 카카오 로그인이 살아 있으면 그 access token (만료됐으면 카카오 SDK 가 갱신), 없으면 null */
    private val kakaoAccessToken: suspend () -> String?,
    override val courseName: String,
    private val now: () -> Long = System::currentTimeMillis,
) : WattagamRepository {
    private val api = WattagamApi(baseUrl, client) { session.load()?.accessToken }
    private val refreshLock = Mutex()
    @Volatile private var placeCache: Map<String, CampusPlace> = emptyMap()
    @Volatile private var stampCache: StampBoardData? = null

    override val connectedToServer = true

    override suspend fun restoreSession(): Account? {
        val saved = session.load()
        if (saved != null && saved.expiresAtMillis - EXPIRY_MARGIN_MILLIS > now()) return Account(saved.nickname)
        val kakao = kakaoAccessToken()
        if (kakao == null) {
            session.clear()
            return null
        }
        return try {
            signIn(kakao)
        } catch (e: ApiException) {
            // 카카오 토큰을 서버가 받아주지 않으면 버튼으로 다시 로그인하게 한다
            if (e.status == 401) null.also { session.clear() } else throw e
        }
    }

    override suspend fun signIn(kakaoAccessToken: String?): Account {
        val account = exchange(requireNotNull(kakaoAccessToken) { "서버 로그인에는 카카오 토큰이 필요합니다" })
        placeCache = emptyMap()
        stampCache = null
        return account
    }

    override suspend fun saveNickname(nickname: String): Account {
        val saved = authorized { api.updateNickname(nickname) }
        session.load()?.let { session.save(it.copy(nickname = saved.nickname)) }
        return Account(saved.nickname)
    }

    override suspend fun logout() {
        try {
            if (session.load() != null) api.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 서버 로그아웃은 기록용이라 실패해도 이 기기에서는 로그아웃한다 (토큰은 1시간 뒤 만료)
        }
        session.clear()
        placeCache = emptyMap()
        stampCache = null
    }

    override suspend fun withdraw() {
        authorized { api.withdraw() }
        session.clear()
        placeCache = emptyMap()
        stampCache = null
    }

    override suspend fun places(): List<CampusPlace> {
        val previous = placeCache
        val list = authorized { api.places() }.places.sortedBy { it.courseOrder }.map {
            val id = it.placeId.toString()
            CampusPlace(id, it.courseOrder, it.name, previous[id]?.checkpoint.orEmpty(), it.latitude, it.longitude, it.stampEarned)
        }
        placeCache = list.associateBy { it.id }
        return list
    }

    override fun cachedPlace(placeId: String): CampusPlace? = placeCache[placeId]

    override suspend fun placeDetail(placeId: String): CampusPlaceDetail = coroutineScope {
        val detail = async { authorized { api.place(placeId.toServerId(PLACE_NOT_FOUND)) } }
        val board = async { stampBoard() }
        val dto = detail.await()
        val stamped = board.await().slots.any { it.placeId == placeId && it.earned }
        val place = CampusPlace(placeId, dto.courseOrder, dto.name, dto.visitPointName, dto.latitude, dto.longitude, stamped)
        placeCache = placeCache + (placeId to place)
        CampusPlaceDetail(place, dto.description, dto.highlight, writingPrompt = "", dto.directions, publicCount = null)
    }

    override fun cachedStampBoard(): StampBoardData? = stampCache

    override suspend fun stampBoard(): StampBoardData {
        val dto = authorized { api.stamps() }
        return StampBoardData(
            dto.earnedCount, dto.totalCount,
            dto.stamps.sortedBy { it.courseOrder }.map { StampSlot(it.placeId.toString(), it.placeName, it.earned) },
        ).also { stampCache = it }
    }

    override suspend fun checkVisit(placeId: String, fix: LocationFix.Found): VisitEligibility {
        val result = authorized { api.visit(placeId.toServerId(PLACE_NOT_FOUND), fix.toBody()) }
        return when {
            result.canWrite -> VisitEligibility.Allowed
            result.reasonCode == "OUTSIDE_RADIUS" -> VisitEligibility.Outside
            // 정확도가 낮거나 오래된 위치: 범위 밖으로 단정하지 않고 다시 재보게 한다
            else -> VisitEligibility.Unavailable
        }
    }

    override suspend fun gallery(placeId: String, page: Int): GuestbookPage {
        val placeName = place(placeId).name
        val dto = authorized { api.gallery(placeId.toServerId(PLACE_NOT_FOUND), page, GALLERY_PAGE_SIZE) }
        return GuestbookPage(
            dto.entries.map { it.toEntry(placeId, placeName, author = it.nickname.orEmpty(), mine = it.mine) },
            dto.hasNext,
        )
    }

    override suspend fun guestbook(entryId: String): GuestbookEntry =
        authorized { api.guestbook(entryId.toServerId(GUESTBOOK_NOT_FOUND)) }.toEntry()

    override suspend fun updateGuestbook(entryId: String, message: String): GuestbookEntry {
        val trimmed = message.trim()
        return authorized { api.updateGuestbook(entryId.toServerId(GUESTBOOK_NOT_FOUND), trimmed.ifEmpty { null }) }.toEntry()
    }

    override suspend fun deleteGuestbook(entryId: String) {
        authorized { api.deleteGuestbook(entryId.toServerId(GUESTBOOK_NOT_FOUND)) }
    }

    override suspend fun myGuestbooks(): GuestbookPage {
        val dto = authorized { api.myGuestbooks(page = 0, size = MY_PAGE_SIZE) }
        val nickname = session.load()?.nickname.orEmpty()
        return GuestbookPage(
            dto.entries.map { it.toEntry(it.placeId?.toString().orEmpty(), it.placeName.orEmpty(), author = nickname, mine = true) },
            dto.hasNext,
        )
    }

    override suspend fun publish(placeId: String, photo: File, message: String, fix: LocationFix.Found, requestId: String): PublishOutcome {
        val dto = authorized { api.createGuestbook(placeId.toServerId(PLACE_NOT_FOUND), photo, message, fix.toBody(), requestId) }
        placeCache[placeId]?.let { placeCache = placeCache + (placeId to it.copy(stamped = true)) }
        stampCache = stampCache?.let { board ->
            StampBoardData(dto.earnedCount, dto.totalCount, board.slots.map { if (it.placeId == placeId) it.copy(earned = true) else it })
        }
        return PublishOutcome.Published(
            dto.entryId.toString(), dto.imageUrl, dto.message.orEmpty(), displayTime(dto.createdAt),
            dto.stampAwarded, dto.earnedCount, dto.totalCount,
        )
    }

    /** 카카오 토큰을 서비스 토큰으로 바꿔 저장한다 */
    private suspend fun exchange(kakaoAccessToken: String): Account {
        val login = api.loginWithKakao(kakaoAccessToken)
        val nickname = if (login.needsNickname) null else login.user.nickname
        session.save(Session(login.accessToken, now() + login.expiresInSeconds * 1000, nickname))
        return Account(nickname)
    }

    private suspend fun <T> authorized(block: suspend () -> T): T {
        val tokenBefore = session.load()?.accessToken ?: throw AccountProblem.SessionExpired()
        return try {
            block()
        } catch (e: ApiException) {
            if (e.status != 401) throw e.asAccountProblem()
            refresh(tokenBefore)
            try {
                block()
            } catch (retry: ApiException) {
                if (retry.status == 401) {
                    session.clear()
                    throw AccountProblem.SessionExpired()
                }
                throw retry.asAccountProblem()
            }
        }
    }

    /** 만료된 토큰을 새로 받는다. 동시에 여러 요청이 401 을 받아도 한 번만 교환한다 */
    private suspend fun refresh(expiredToken: String) = refreshLock.withLock {
        if (session.load()?.accessToken != expiredToken) return@withLock
        val kakao = kakaoAccessToken()
        if (kakao == null) {
            session.clear()
            throw AccountProblem.SessionExpired()
        }
        try {
            exchange(kakao)
        } catch (e: ApiException) {
            if (e.status == 401) {
                session.clear()
                throw AccountProblem.SessionExpired()
            }
            throw e
        }
    }

    private fun ApiException.asAccountProblem(): Exception {
        if (code != NICKNAME_REQUIRED) return this
        session.load()?.let { session.save(it.copy(nickname = null)) }
        return AccountProblem.NicknameRequired()
    }

    private fun GuestbookItemDto.toEntry(placeId: String, placeName: String, author: String, mine: Boolean) = GuestbookEntry(
        entryId.toString(), placeId, placeName, author, title = null, message.orEmpty(), imageUrl, displayTime(createdAt), mine, edited,
    )

    private fun GuestbookDetailDto.toEntry() = GuestbookEntry(
        entryId.toString(), placeId.toString(), placeName, nickname, title = null,
        message.orEmpty(), imageUrl, displayTime(createdAt), mine, edited,
    )

    private companion object {
        /** 만료 직전 토큰은 쓰지 않는다 */
        const val EXPIRY_MARGIN_MILLIS = 60_000L
        const val GALLERY_PAGE_SIZE = 20
        /** 서버 최대 크기. 마이페이지는 한 번에 받아 장소별로 거른다 */
        const val MY_PAGE_SIZE = 50
        const val NICKNAME_REQUIRED = "USER_403_1"
        const val PLACE_NOT_FOUND = "PLACE_404_1"
        const val GUESTBOOK_NOT_FOUND = "GUESTBOOK_404_1"
    }
}

/** 등록 때 위치 때문에 거절되면 그 이유 (방문 확인과 같은 기준), 위치 문제가 아니면 null */
fun ApiException.locationRejection(): VisitEligibility? = when (code) {
    "GUESTBOOK_403_1" -> VisitEligibility.Outside
    "GUESTBOOK_403_2", "GUESTBOOK_403_3" -> VisitEligibility.Unavailable
    else -> null
}

private fun String.toServerId(notFoundCode: String): Long = toLongOrNull() ?: throw ApiException(404, notFoundCode, null)

private fun LocationFix.Found.toBody() = LocationBody(
    latitude, longitude,
    accuracyMeters = round(accuracyMeters * 10.0) / 10.0,
    measuredAt = Instant.ofEpochMilli(measuredAtMillis).toString(),
)

private val displayFormat = DateTimeFormatter.ofPattern("yyyy. M. d. HH:mm")
private val korea = ZoneId.of("Asia/Seoul")

/** 서버의 UTC 시각을 한국 시간 표시로 (예: 2026. 11. 6. 17:00). 형식이 다르면 받은 그대로 */
internal fun displayTime(iso: String): String =
    runCatching { Instant.parse(iso).atZone(korea).format(displayFormat) }.getOrDefault(iso)
