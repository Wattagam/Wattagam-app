package com.wattagam.user

import com.wattagam.common.api.ApiResponse
import com.wattagam.common.security.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me")
class UserController(
    private val userService: UserService,
    private val withdrawalService: UserWithdrawalService,
) {

    @PatchMapping("/nickname")
    fun updateNickname(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Valid @RequestBody request: UserNicknameRequest,
    ): ApiResponse<UserNicknameResponse> {
        userService.updateNickname(authenticatedUser.userId, request.nickname)
        val response = UserNicknameResponse(authenticatedUser.userId, request.nickname)
        return ApiResponse.success("USER_200_1", "닉네임을 저장했습니다.", response)
    }

    @DeleteMapping
    fun withdraw(@AuthenticationPrincipal authenticatedUser: AuthenticatedUser): ApiResponse<Nothing> {
        withdrawalService.withdraw(authenticatedUser.userId)
        return ApiResponse.success("USER_200_2", "회원 탈퇴가 완료되었습니다.")
    }
}
