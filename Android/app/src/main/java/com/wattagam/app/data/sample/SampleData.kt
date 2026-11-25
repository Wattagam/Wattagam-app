package com.wattagam.app.data.sample

/**
 * API 연결 전 화면 확인용 샘플 데이터.
 * 실제 로그인·GPS·도장 상태가 아니며, API 단계에서 data/place, data/guestbook 의 Repository 로 대체한다.
 */
data class SamplePlace(
    val id: String,
    val order: Int,
    val name: String,
    val description: String,
    val highlight: String,
    val writingPrompt: String,
    val directions: String,
    val checkpoint: String,
    /** 방문 판정 중심 좌표와 반경. 서버 초기 데이터(V3)와 같은 값 */
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val stamped: Boolean,
)

data class SampleGuestbook(
    val id: String,
    val placeId: String,
    val title: String,
    val message: String,
    val author: String,
    /** 화면 표시용 작성 시각 (예: 2026. 10. 27. 14:32) */
    val createdAt: String,
    val mine: Boolean,
)

object SampleData {
    const val NICKNAME = "하늘"
    const val COURSE = "푸른캠퍼스 산책"

    val places = listOf(
        SamplePlace(
            id = "310", order = 1, name = "310관",
            description = "수업과 동아리 활동이 이어지는 캠퍼스의 중심 건물이에요. 로비와 앞 광장에 오가는 학생들의 일상이 쌓여요.",
            highlight = "건물 앞 광장과 로비",
            writingPrompt = "수업 사이 쉬어가던 풍경에 짧은 마음을 남겨요.",
            directions = "청룡연못에서 언덕길을 따라 올라가면 310관 앞 광장이에요.",
            checkpoint = "310관 정문 앞 광장",
            latitude = 37.503645, longitude = 126.956032, radiusMeters = 75,
            stamped = true,
        ),
        SamplePlace(
            id = "library", order = 2, name = "도서관",
            description = "커다란 창 너머로 오후의 햇살이 길게 들어오는 곳이에요. 도서관 앞 계단에 잠시 앉아, 책과 사람들 사이의 고요를 느껴보세요.",
            highlight = "계단에서 바라보는 창가의 빛과 그림자",
            writingPrompt = "오늘의 풍경에 손그림과 짧은 메시지를 더해보세요.",
            directions = "정문에서 본관 오른쪽으로 가면 넓은 계단이 보여요.",
            checkpoint = "도서관 앞 계단",
            latitude = 37.504779, longitude = 126.957959, radiusMeters = 40,
            stamped = true,
        ),
        SamplePlace(
            id = "pond", order = 3, name = "청룡연못",
            description = "청룡상이 지키는 연못이에요. 물가 벤치에 앉아 잔잔한 수면에 비친 캠퍼스를 바라보세요.",
            highlight = "연못 가운데 청룡상",
            writingPrompt = "산책 중 마음에 남은 풍경은 무엇인가요?",
            directions = "도서관 옆 길을 따라 내려오면 연못이 보여요.",
            checkpoint = "청룡연못 앞",
            latitude = 37.505625, longitude = 126.957321, radiusMeters = 30,
            stamped = false,
        ),
    )

    val guestbooks = listOf(
        SampleGuestbook("g1", "library", "햇살까지 기억할 오후", "책장 사이의 햇살, 다음에도 이 자리에서 만나자.", NICKNAME, "2026. 10. 27. 14:32", mine = true),
        SampleGuestbook("g2", "library", "창가에서 보낸 시간", "창가에 앉아 책을 읽다 보니, 어느새 해가 기울었다. 다음 산책에도 꼭 들러야지.", "유진", "2026. 10. 26. 16:10", mine = false),
        SampleGuestbook("g3", "library", "우리의 단골 자리", "시험 기간마다 앉던 자리. 오늘은 여유롭게.", "서연", "2026. 10. 25. 12:00", mine = false),
        SampleGuestbook("g4", "library", "책 한 권만큼의 여유", "", "민준", "2026. 10. 24. 11:00", mine = false),
        SampleGuestbook("g5", "library", "도서관의 오후", "계단에 앉아 쉬어 가기 좋은 날.", "지우", "2026. 10. 23. 10:00", mine = false),
        SampleGuestbook("g6", "library", "늦가을 창가", "", "서연", "2026. 10. 22. 15:20", mine = false),
        SampleGuestbook("g7", "310", "산책의 시작은 가볍게", "310관 앞 광장에서 출발!", NICKNAME, "2026. 10. 27. 13:58", mine = true),
        SampleGuestbook("g8", "310", "광장 벤치에서", "", "서연", "2026. 10. 22. 09:30", mine = false),
        SampleGuestbook("g9", "pond", "물 위에 비친 하늘", "청룡상 앞에서 한 장.", "민준", "2026. 10. 21. 17:00", mine = false),
    )

    fun place(id: String): SamplePlace? = places.find { it.id == id }

    fun guestbook(id: String): SampleGuestbook? = guestbooks.find { it.id == id }

    fun guestbooksAt(placeId: String): List<SampleGuestbook> = guestbooks.filter { it.placeId == placeId }

    fun myGuestbooks(): List<SampleGuestbook> = guestbooks.filter { it.mine }
}
