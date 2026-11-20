package com.wattagam.guestbook

import com.wattagam.auth.JwtService
import com.wattagam.stamp.StampRepository
import com.wattagam.stamp.StampService
import com.wattagam.user.User
import com.wattagam.user.UserRepository
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.PlatformTransactionManager
import org.testcontainers.mysql.MySQLContainer

/**
 * 방명록 통합 테스트가 공유하는 MySQL 컨테이너와 데이터 준비 도구.
 * 스프링 컨텍스트가 클래스 사이에 재사용되므로 컨테이너도 JVM 전체에서 하나만 띄운다.
 */
@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.auth.kakao.app-id=12345",
    ],
)
@AutoConfigureMockMvc
@Import(FakeImageStorageConfiguration::class)
abstract class GuestbookIntegrationTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var guestbookEntryRepository: GuestbookEntryRepository

    @Autowired
    protected lateinit var cleanupRepository: GuestbookImageCleanupRepository

    @Autowired
    protected lateinit var stampRepository: StampRepository

    @Autowired
    protected lateinit var stampService: StampService

    @Autowired
    protected lateinit var jwtService: JwtService

    @Autowired
    protected lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    protected lateinit var fakeStorage: FakeObjectStorage

    @BeforeEach
    fun clearTestData() {
        userRepository.deleteAll()
        cleanupRepository.deleteAll()
        fakeStorage.reset()
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

    protected fun jpegBytes(width: Int = 64, height: Int = 64): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", output)
        return output.toByteArray()
    }

    protected fun bearer(userId: Long): String = "Bearer ${jwtService.issueAccessToken(userId)}"

    protected fun authorization(userId: Long): Pair<String, String> = HttpHeaders.AUTHORIZATION to bearer(userId)

    companion object {
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer = MySQLContainer("mysql:8.4.13")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_0900_ai_ci",
                "--default-time-zone=+00:00",
            )
            .also { it.start() }
    }
}
