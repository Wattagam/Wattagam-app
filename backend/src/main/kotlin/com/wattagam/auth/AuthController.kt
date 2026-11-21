package com.wattagam.auth

import com.wattagam.auth.kakao.KakaoClient
import com.wattagam.common.api.ApiResponse
import com.wattagam.user.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakao: KakaoClient,
    private val users: UserService,
    private val jwt: JwtService,
    private val properties: JwtProperties,
) {

    @PostMapping("/kakao")
    fun login(@Valid @RequestBody request: KakaoLoginRequest): ApiResponse<LoginResponse> {
        val login = users.findOrCreateKakaoUser(kakao.verifyAccessToken(request.kakaoAccessToken))
        val userId = requireNotNull(login.user.id) { "저장된 사용자 ID가 없습니다." }
        val result = LoginResponse(
            jwt.issueAccessToken(userId), "Bearer", properties.expiresIn,
            login.isNewUser, login.user.nickname == null, LoginUserResponse(userId, login.user.nickname),
        )
        return ApiResponse.success("AUTH_200_1", "로그인에 성공했습니다.", result)
    }

    @PostMapping("/logout")
    fun logout(): ApiResponse<Nothing> =
        ApiResponse.success("AUTH_200_2", "로그아웃 요청을 처리했습니다.")
}

data class KakaoLoginRequest(
    @field:NotBlank(message = "카카오 인증 정보를 입력해 주세요.")
    val kakaoAccessToken: String,
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val isNewUser: Boolean,
    val needsNickname: Boolean,
    val user: LoginUserResponse,
)

data class LoginUserResponse(val userId: Long, val nickname: String?)
