package com.wattagam.common.api

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        failure(exception.errorCode)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = if (exception.bindingResult.fieldErrors.any { it.field == "nickname" }) {
            ErrorCode.INVALID_NICKNAME
        } else {
            ErrorCode.INVALID_REQUEST
        }
        return failure(errorCode)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(): ResponseEntity<ApiResponse<Nothing>> = failure(ErrorCode.INVALID_REQUEST)

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ApiResponse<Nothing>> {
        if (exception is ErrorResponse && exception.statusCode.is4xxClientError) {
            val status = exception.statusCode.value()
            val response = ApiResponse<Nothing>(false, status, "COMMON_${status}_1", "요청을 처리할 수 없습니다.", null)
            return ResponseEntity.status(status).body(response)
        }
        logger.error("요청 처리에 실패했습니다. 오류 유형: {}", exception.javaClass.simpleName)
        return failure(ErrorCode.INTERNAL_ERROR)
    }

    private fun failure(errorCode: ErrorCode): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(errorCode.status).body(ApiResponse.failure(errorCode))
}
