package com.wattagam.place

import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal
import java.time.Duration
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.visit")
class VisitProperties(
    @field:DecimalMin(value = "0", inclusive = false, message = "허용 GPS 정확도는 0m보다 커야 합니다.")
    val maxAccuracyMeters: BigDecimal,
    @field:DurationMin(seconds = 1, message = "GPS 유효 시간은 1초 이상이어야 합니다.")
    val maxAge: Duration,
    @field:DurationMin(seconds = 0, message = "GPS 미래 시각 허용 오차는 음수일 수 없습니다.")
    val futureTolerance: Duration,
)
