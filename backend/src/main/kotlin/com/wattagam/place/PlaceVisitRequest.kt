package com.wattagam.place

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class PlaceVisitRequest(
    @field:DecimalMin("-90")
    @field:DecimalMax("90")
    val latitude: BigDecimal,
    @field:DecimalMin("-180")
    @field:DecimalMax("180")
    val longitude: BigDecimal,
    @field:DecimalMin("0")
    val accuracyMeters: BigDecimal,
    @field:NotBlank
    @field:Size(max = 64)
    val measuredAt: String,
)
