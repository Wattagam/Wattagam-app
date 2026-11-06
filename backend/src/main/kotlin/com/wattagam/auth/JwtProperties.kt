package com.wattagam.auth

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.auth.jwt")
class JwtProperties(
    @field:NotBlank(message = "서비스 JWT 서명 키를 설정해 주세요.")
    val secret: String,
    @field:Min(value = 1, message = "서비스 JWT 유효기간은 1초 이상이어야 합니다.")
    val expiresIn: Long = 3600,
    val issuer: String = "wattagam",
    val audience: String = "wattagam-api",
)
