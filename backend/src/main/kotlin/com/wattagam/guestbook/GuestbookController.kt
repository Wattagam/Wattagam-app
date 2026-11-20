package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ApiResponse
import com.wattagam.common.api.ErrorCode
import com.wattagam.common.security.AuthenticatedUser
import com.wattagam.place.PlaceVisitRequest
import java.math.BigDecimal
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class GuestbookController(private val guestbookService: GuestbookService) {

    @PostMapping("/api/places/{id}/guestbook", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable id: Long,
        @RequestPart("image", required = false) image: MultipartFile?,
        @RequestParam("message", required = false) message: String?,
        @RequestParam("latitude", required = false) latitude: BigDecimal?,
        @RequestParam("longitude", required = false) longitude: BigDecimal?,
        @RequestParam("accuracyMeters", required = false) accuracyMeters: BigDecimal?,
        @RequestParam("measuredAt", required = false) measuredAt: String?,
        @RequestParam("clientRequestId", required = false) clientRequestId: String?,
    ): ResponseEntity<ApiResponse<GuestbookCreateResponse>> {
        // 누락된 필수 값은 공통 400으로 통일하기 위해 서비스 호출 전에 직접 확인한다.
        val command = GuestbookCreateCommand(
            image = image?.takeIf { !it.isEmpty }?.bytes ?: throw ApiException(ErrorCode.INVALID_REQUEST),
            message = message?.takeIf { it.isNotEmpty() },
            location = PlaceVisitRequest(
                latitude = latitude ?: throw ApiException(ErrorCode.INVALID_REQUEST),
                longitude = longitude ?: throw ApiException(ErrorCode.INVALID_REQUEST),
                accuracyMeters = accuracyMeters ?: throw ApiException(ErrorCode.INVALID_REQUEST),
                measuredAt = measuredAt ?: throw ApiException(ErrorCode.INVALID_REQUEST),
            ),
            clientRequestId = clientRequestId ?: throw ApiException(ErrorCode.INVALID_REQUEST),
        )
        return toCreateResponse(guestbookService.create(user.userId, id, command))
    }

    private fun toCreateResponse(result: GuestbookCreateResult): ResponseEntity<ApiResponse<GuestbookCreateResponse>> {
        val body = if (result.created) {
            ApiResponse(true, 201, "GUESTBOOK_201_1", "방명록을 등록했습니다.", result.response)
        } else {
            ApiResponse(true, 200, "GUESTBOOK_200_1", "이미 등록된 방명록입니다.", result.response)
        }
        return ResponseEntity.status(body.status).body(body)
    }
}
