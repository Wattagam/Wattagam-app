package com.wattagam.auth.kakao

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "app.auth.kakao")
data class KakaoProperties(
    @field:Min(value = 1, message = "카카오 앱 ID는 1 이상이어야 합니다.")
    val appId: Long,
    val baseUrl: String = "https://kapi.kakao.com",
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(5),
) {

    init {
        require(connectTimeout.toMillis() in 1L..Int.MAX_VALUE.toLong()) {
            "카카오 연결 제한 시간은 1ms 이상, 2147483647ms 이하여야 합니다."
        }
        require(readTimeout.toMillis() in 1L..Int.MAX_VALUE.toLong()) {
            "카카오 응답 제한 시간은 1ms 이상, 2147483647ms 이하여야 합니다."
        }
    }
}
