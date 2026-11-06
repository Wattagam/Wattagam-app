package com.wattagam.common.api

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val code: String, val message: String) {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400_2", "요청 형식 또는 필드 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401_1", "인증 정보가 없거나 유효하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403_1", "해당 요청을 처리할 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_1", "서버 오류가 발생했습니다."),
    KAKAO_UNAUTHORIZED(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_1",
        "카카오 인증 정보가 유효하지 않거나 해당 앱에서 발급된 토큰이 아닙니다.",
    ),
    KAKAO_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_503_1", "카카오 인증을 일시적으로 처리할 수 없습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER_400_1", "닉네임은 한글, 영문, 숫자로 2자 이상 10자 이하로 입력해 주세요."),
    NICKNAME_REQUIRED(HttpStatus.FORBIDDEN, "USER_403_1", "닉네임 설정이 필요합니다."),
}
