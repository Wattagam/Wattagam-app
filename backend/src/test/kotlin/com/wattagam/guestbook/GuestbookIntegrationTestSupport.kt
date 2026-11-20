package com.wattagam.guestbook

import com.wattagam.auth.JwtService
import com.wattagam.stamp.StampRepository
import com.wattagam.stamp.StampService
import com.wattagam.user.User
import com.wattagam.user.UserRepository
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.PlatformTransactionManager
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer

/** 방명록 통합 테스트가 공유하는 MySQL 컨테이너와 데이터 준비 도구. */
@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.auth.kakao.app-id=12345",
    ],
)
@AutoConfigureMockMvc
@Testcontainers
abstract class GuestbookIntegrationTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var guestbookEntryRepository: GuestbookEntryRepository

    @Autowired
    protected lateinit var stampRepository: StampRepository

    @Autowired
    protected lateinit var stampService: StampService

    @Autowired
    protected lateinit var jwtService: JwtService

    @Autowired
    protected lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    fun clearTestUsers() {
        userRepository.deleteAll()
    }

    protected fun createUser(kakaoUserId: Long, nickname: String? = "푸앙이"): Long {
        val user = userRepository.save(User(kakaoUserId = kakaoUserId, nickname = nickname, createdAt = Instant.now()))
        return requireNotNull(user.id)
    }

    protected fun createEntry(
        userId: Long,
        placeId: Long = 1L,
        message: String? = null,
        createdAt: Instant = Instant.now(),
        imageObjectKey: String = "guestbook/${UUID.randomUUID()}.jpg",
    ): GuestbookEntry = guestbookEntryRepository.save(
        GuestbookEntry(
            placeId = placeId,
            userId = userId,
            imageObjectKey = imageObjectKey,
            message = message,
            clientRequestId = UUID.randomUUID().toString(),
            createdAt = createdAt,
            updatedAt = createdAt,
        ),
    )

    protected fun bearer(userId: Long): String = "Bearer ${jwtService.issueAccessToken(userId)}"

    protected fun authorization(userId: Long): Pair<String, String> = HttpHeaders.AUTHORIZATION to bearer(userId)

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4.13")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_0900_ai_ci",
                "--default-time-zone=+00:00",
            )
    }
}
