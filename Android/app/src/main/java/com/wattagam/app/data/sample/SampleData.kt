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
    /** 방문 확인 지점 좌표. 지도에서 대략 잡은 값이라 현장에서 측정해 바꿔야 함 (반경 30 m 판정에 쓰임) */
    val latitude: Double,
    val longitude: Double,
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
            id = "gate", order = 1, name = "정문",
            description = "정문을 지나면 오래된 느티나무가 산책의 시작을 반겨요. 나뭇잎 사이로 내려오는 빛과 학생들의 발걸음을 천천히 따라가 보세요.",
            highlight = "고개를 들면 만나는 초록빛 나무 터널",
            writingPrompt = "오늘 산책을 시작하는 마음을 한 문장으로 남겨보세요.",
            directions = "정문 안내석에서 안쪽으로 30m, 첫 번째 벤치 옆이에요.",
            checkpoint = "정문 안내석",
            latitude = 37.50475, longitude = 126.95765,
            stamped = true,
        ),
        SamplePlace(
            id = "library", order = 2, name = "도서관",
            description = "커다란 창 너머로 오후의 햇살이 길게 들어오는 곳이에요. 도서관 앞 계단에 잠시 앉아, 책과 사람들 사이의 고요를 느껴보세요.",
            highlight = "계단에서 바라보는 창가의 빛과 그림자",
            writingPrompt = "오늘의 풍경에 손그림과 짧은 메시지를 더해보세요.",
            directions = "정문에서 본관 오른쪽으로 가면 넓은 계단이 보여요.",
            checkpoint = "도서관 앞 계단",
            latitude = 37.50620, longitude = 126.95810,
            stamped = true,
        ),
        SamplePlace(
            id = "pond", order = 3, name = "청룡탕",
            description = "청룡상이 지키는 연못이에요. 물가 벤치에 앉아 잔잔한 수면에 비친 캠퍼스를 바라보세요.",
            highlight = "연못 가운데 청룡상",
            writingPrompt = "산책 중 마음에 남은 풍경은 무엇인가요?",
            directions = "도서관 옆 길을 따라 내려오면 연못이 보여요.",
            checkpoint = "청룡탕 연못 앞",
            latitude = 37.50560, longitude = 126.95740,
            stamped = false,
        ),
        SamplePlace(
            id = "310", order = 4, name = "310관",
            description = "수업과 동아리 활동이 이어지는 캠퍼스의 중심 건물이에요. 로비와 앞 광장에 오가는 학생들의 일상이 쌓여요.",
            highlight = "건물 앞 광장과 로비",
            writingPrompt = "수업 사이 쉬어가던 풍경에 짧은 마음을 남겨요.",
            directions = "청룡탕에서 언덕길을 따라 올라가면 310관 앞 광장이에요.",
            checkpoint = "310관 정문 앞 광장",
            latitude = 37.50395, longitude = 126.95400,
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
        SampleGuestbook("g7", "gate", "산책의 시작은 가볍게", "초록 터널 아래에서 출발!", NICKNAME, "2026. 10. 27. 13:58", mine = true),
        SampleGuestbook("g8", "gate", "초록 터널 아래에서", "", "서연", "2026. 10. 22. 09:30", mine = false),
        SampleGuestbook("g9", "pond", "물 위에 비친 하늘", "청룡상 앞에서 한 장.", "민준", "2026. 10. 21. 17:00", mine = false),
    )

    fun place(id: String): SamplePlace? = places.find { it.id == id }

    fun guestbook(id: String): SampleGuestbook? = guestbooks.find { it.id == id }

    fun guestbooksAt(placeId: String): List<SampleGuestbook> = guestbooks.filter { it.placeId == placeId }

    fun myGuestbooks(): List<SampleGuestbook> = guestbooks.filter { it.mine }
}
