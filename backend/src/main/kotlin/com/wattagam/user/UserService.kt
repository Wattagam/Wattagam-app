package com.wattagam.user

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class UserService(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    private val nicknameRegex = Regex(NICKNAME_PATTERN)

    fun findOrCreateKakaoUser(kakaoUserId: Long): UserLogin {
        userRepository.findByKakaoUserId(kakaoUserId)?.let { return UserLogin(it, false) }

        return try {
            val user = User(kakaoUserId = kakaoUserId, createdAt = clock.instant())
            UserLogin(userRepository.saveAndFlush(user), true)
        } catch (exception: DataIntegrityViolationException) {
            // 생성 트랜잭션이 롤백된 뒤 동시 로그인으로 먼저 저장된 계정을 조회한다.
            val user = userRepository.findByKakaoUserId(kakaoUserId) ?: throw exception
            UserLogin(user, false)
        }
    }

    fun findByIdOrNull(userId: Long): User? = userRepository.findById(userId).orElse(null)

    @Transactional
    fun updateNickname(userId: Long, nickname: String): User {
        val user = findByIdOrNull(userId) ?: throw ApiException(ErrorCode.UNAUTHORIZED)
        if (!nicknameRegex.matches(nickname)) {
            throw ApiException(ErrorCode.INVALID_NICKNAME)
        }
        user.nickname = nickname
        return user
    }

    companion object {
        const val NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]{2,10}$"
    }
}
