package com.wattagam.guestbook

import com.wattagam.place.PlaceVisitRequest

/** 컨트롤러가 검증한 방명록 작성 입력. 위치는 작성 시점 재검증에 그대로 쓴다. */
class GuestbookCreateCommand(
    val image: ByteArray,
    val message: String?,
    val location: PlaceVisitRequest,
    val clientRequestId: String,
)
