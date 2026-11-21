package com.wattagam.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserNicknameRequest(
    @field:NotBlank(message = "닉네임을 입력해 주세요.")
    @field:Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해 주세요.")
    @field:Pattern(regexp = UserService.NICKNAME_PATTERN, message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
    val nickname: String,
)
