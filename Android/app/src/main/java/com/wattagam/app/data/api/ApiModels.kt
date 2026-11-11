package com.wattagam.app.data.api

import kotlinx.serialization.Serializable

/*
 * 왔다감 서버 응답 모델. 서버 명세(노션 API 명세, backend develop)의 필드 이름을 그대로 쓴다.
 * 모르는 필드는 무시하고, 앱이 쓰지 않는 필드는 담지 않는다.
 */

/** 모든 응답을 감싸는 공통 형식. 실패하면 result 는 null 이고 code·message 에 이유가 온다 */
@Serializable
internal data class Envelope<T>(val code: String? = null, val message: String? = null, val result: T? = null)

@Serializable
internal data class KakaoLoginBody(val kakaoAccessToken: String)

@Serializable
data class LoginDto(
    val accessToken: String,
    val expiresInSeconds: Long,
    val needsNickname: Boolean,
    val user: LoginUserDto,
)

@Serializable
data class LoginUserDto(val userId: Long, val nickname: String? = null)

@Serializable
internal data class NicknameBody(val nickname: String)

@Serializable
data class NicknameDto(val userId: Long, val nickname: String)

@Serializable
data class PlaceListDto(val places: List<PlaceSummaryDto>)

@Serializable
data class PlaceSummaryDto(
    val placeId: Long,
    val courseOrder: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val visitRadiusMeters: Int,
    val stampEarned: Boolean,
)

@Serializable
data class PlaceDetailDto(
    val placeId: Long,
    val courseOrder: Int,
    val name: String,
    val description: String,
    val highlight: String,
    val directions: String,
    val visitPointName: String,
    val latitude: Double,
    val longitude: Double,
    val visitRadiusMeters: Int,
)

/** 현재 위치. measuredAt 은 위치를 잰 UTC 시각(ISO 8601) */
@Serializable
data class LocationBody(val latitude: Double, val longitude: Double, val accuracyMeters: Double, val measuredAt: String)

@Serializable
data class VisitDto(val placeId: Long, val canWrite: Boolean, val reasonCode: String? = null)

@Serializable
data class StampBoardDto(val earnedCount: Int, val totalCount: Int, val stamps: List<StampSlotDto>)

@Serializable
data class StampSlotDto(val placeId: Long, val courseOrder: Int, val placeName: String, val earned: Boolean)

@Serializable
data class GuestbookCreatedDto(
    val entryId: Long,
    val placeId: Long,
    val imageUrl: String,
    val message: String? = null,
    val createdAt: String,
    val stampAwarded: Boolean,
    val earnedCount: Int,
    val totalCount: Int,
)

/** 갤러리·내 기록 목록의 한 칸. 갤러리에는 nickname·mine, 내 기록에는 placeId·placeName 이 온다 */
@Serializable
data class GuestbookItemDto(
    val entryId: Long,
    val placeId: Long? = null,
    val placeName: String? = null,
    val nickname: String? = null,
    val imageUrl: String,
    val message: String? = null,
    val createdAt: String,
    val edited: Boolean = false,
    val mine: Boolean = false,
)

@Serializable
data class GuestbookPageDto(val entries: List<GuestbookItemDto>, val hasNext: Boolean)

@Serializable
data class GuestbookDetailDto(
    val entryId: Long,
    val placeId: Long,
    val placeName: String,
    val nickname: String,
    val imageUrl: String,
    val message: String? = null,
    val createdAt: String,
    val edited: Boolean = false,
    val mine: Boolean,
)

/** 메시지 수정. null 이면 메시지를 지운다 (필드 자체는 꼭 보내야 해서 null 도 그대로 보낸다) */
@Serializable
internal data class GuestbookMessageBody(val message: String?)
