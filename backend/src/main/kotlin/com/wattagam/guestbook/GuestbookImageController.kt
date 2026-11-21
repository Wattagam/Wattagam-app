package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ApiResponse
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GuestbookImageController(private val guestbookImageService: GuestbookImageService) {

    @GetMapping("/api/guestbook/{id}/image")
    fun getImage(@PathVariable id: Long): ResponseEntity<ByteArray> = ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .cacheControl(CacheControl.noStore().cachePrivate())
        .body(guestbookImageService.read(id))

    // 이미지 로더는 Accept: image/*를 보내므로, 실패 응답의 JSON 형식을 명시해 406으로 바뀌지 않게 한다.
    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(exception.errorCode.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiResponse.failure(exception.errorCode))
}
