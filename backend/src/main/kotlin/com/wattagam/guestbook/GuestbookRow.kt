package com.wattagam.guestbook

import java.time.Instant

/** 방명록과 장소명, 작성자 닉네임을 한 번에 읽는 조회 전용 행. 이미지 객체 키는 담지 않는다. */
data class GuestbookRow(
    val entryId: Long,
    val placeId: Long,
    val placeName: String,
    val authorId: Long,
    val nickname: String?,
    val message: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    // 수정 시각이 작성 시각보다 늦으면 '수정됨'으로 표시한다.
    val edited: Boolean get() = updatedAt.isAfter(createdAt)
}
