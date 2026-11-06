package com.wattagam.common.api

import java.time.Instant

data class ApiResponse<T>(
    val isSuccess: Boolean,
    val status: Int,
    val code: String,
    val message: String,
    val result: T?,
    val timeStamp: Instant = Instant.now(),
) {
    companion object {
        fun <T> success(code: String, message: String, result: T? = null): ApiResponse<T> =
            ApiResponse(true, 200, code, message, result)

        fun failure(errorCode: ErrorCode): ApiResponse<Nothing> =
            ApiResponse(false, errorCode.status.value(), errorCode.code, errorCode.message, null)
    }
}
