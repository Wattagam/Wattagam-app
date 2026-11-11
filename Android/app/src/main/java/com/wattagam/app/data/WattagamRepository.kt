package com.wattagam.app.data

import com.wattagam.app.data.api.ApiException
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.location.VisitEligibility
import java.io.File

/** 로그인한 계정. [nickname] 이 null 이면 닉네임을 먼저 정해야 한다 */
data class Account(val nickname: String?)

data class CampusPlace(
    val id: String,
    val order: Int,
    val name: String,
    /** 방문 확인 지점 이름. 목록 응답에는 없어서 상세를 받기 전에는 빈 값일 수 있다 */
    val checkpoint: String,
    val latitude: Double,
    val longitude: Double,
    val stamped: Boolean,
)

data class CampusPlaceDetail(
    val place: CampusPlace,
    val description: String,
    val highlight: String,
    /** 서버에는 아직 없는 항목이라 서버 연결 시 빈 값 */
    val writingPrompt: String,
    val directions: String,
    /** 공개 기록 수. 서버는 전체 수를 주지 않아서 null */
    val publicCount: Int?,
)

data class StampSlot(val placeId: String, val name: String, val earned: Boolean)

data class StampBoardData(val earned: Int, val total: Int, val slots: List<StampSlot>)

data class GuestbookEntry(
    val id: String,
    val placeId: String,
    val placeName: String,
    val author: String,
    /** 샘플 기록에만 있는 제목. 서버 기록은 메시지가 카드 제목을 대신한다 */
    val title: String?,
    val message: String,
    val imageUrl: String?,
    /** 한국 시간으로 바꾼 표시용 작성 시각 (예: 2026. 10. 27. 14:32) */
    val createdAt: String,
    val mine: Boolean,
    /** 작성 후 메시지를 고친 기록 ('수정됨' 표시) */
    val edited: Boolean = false,
)

data class GuestbookPage(val entries: List<GuestbookEntry>, val hasNext: Boolean)

sealed interface PublishOutcome {
    data class Published(
        val entryId: String,
        val imageUrl: String,
        val message: String,
        val createdAt: String,
        val stampAwarded: Boolean,
        val earnedCount: Int,
        val totalCount: Int,
    ) : PublishOutcome

    /** 샘플 모드: 서버가 없어 등록하지 않았다 (초안은 그대로) */
    data object NotConnected : PublishOutcome
}

/** 다시 로그인하거나 닉네임을 정해야 해서 요청을 이어갈 수 없을 때. 화면은 로그인 흐름으로 보낸다 */
sealed class AccountProblem : Exception() {
    class SessionExpired : AccountProblem()
    class NicknameRequired : AccountProblem()
}

/**
 * 화면이 쓰는 데이터 창구. 서버 주소(local.properties 의 WATTAGAM_API_URL)가 있으면 서버,
 * 없으면 샘플 데이터를 쓴다. 실패는 예외로 올린다: 서버 거절은 ApiException, 연결 실패는 IOException, 계정 문제는 [AccountProblem].
 */
interface WattagamRepository {
    /** 서버에 연결된 상태인지 (false 면 화면에 샘플임을 드러낸다) */
    val connectedToServer: Boolean
    val courseName: String

    /** 저장된 로그인이 아직 쓸 수 있으면 그 계정 (자동 로그인), 아니면 null */
    suspend fun restoreSession(): Account?

    /** 카카오 로그인 결과로 서비스에 로그인한다. 카카오 앱 키가 없는 샘플 모드에서는 [kakaoAccessToken] 이 null */
    suspend fun signIn(kakaoAccessToken: String?): Account
    suspend fun saveNickname(nickname: String): Account
    suspend fun logout()
    suspend fun withdraw()

    suspend fun places(): List<CampusPlace>

    /** 이미 받아 둔 장소 (화면을 넘어갈 때 다시 불러오지 않으려고) */
    fun cachedPlace(placeId: String): CampusPlace?
    suspend fun place(placeId: String): CampusPlace =
        cachedPlace(placeId) ?: places().firstOrNull { it.id == placeId } ?: throw ApiException(404, "PLACE_404_1", null)
    suspend fun placeDetail(placeId: String): CampusPlaceDetail

    fun cachedStampBoard(): StampBoardData?
    suspend fun stampBoard(): StampBoardData

    /** 지금 위치로 이 장소에 방명록을 쓸 수 있는지 (최종 판정은 등록할 때 서버가 다시 한다) */
    suspend fun checkVisit(placeId: String, fix: LocationFix.Found): VisitEligibility

    suspend fun gallery(placeId: String, page: Int): GuestbookPage
    suspend fun guestbook(entryId: String): GuestbookEntry
    suspend fun myGuestbooks(): GuestbookPage

    /** 내 기록의 메시지를 바꾼다. 빈 메시지는 지우기. 수정으로 도장은 바뀌지 않는다 */
    suspend fun updateGuestbook(entryId: String, message: String): GuestbookEntry

    /** 내 기록을 지운다. 받은 도장은 남는다 */
    suspend fun deleteGuestbook(entryId: String)
    suspend fun publish(placeId: String, photo: File, message: String, fix: LocationFix.Found, requestId: String): PublishOutcome
}
