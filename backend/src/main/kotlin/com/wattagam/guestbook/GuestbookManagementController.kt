package com.wattagam.guestbook

import com.wattagam.common.api.ApiException
import com.wattagam.common.api.ApiResponse
import com.wattagam.common.api.ErrorCode
import com.wattagam.common.security.AuthenticatedUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GuestbookManagementController(private val managementService: GuestbookManagementService) {

    @PatchMapping("/api/guestbook/{id}")
    fun updateMessage(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any?>,
    ): ApiResponse<GuestbookDetailResponse> {
        // message 키가 없는 요청과 null로 지우는 요청을 구분하려고 본문을 Map으로 받는다.
        if (!body.containsKey("message")) {
            throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        val message = when (val raw = body["message"]) {
            null -> null
            is String -> raw
            else -> throw ApiException(ErrorCode.INVALID_REQUEST)
        }
        return ApiResponse.success("GUESTBOOK_200_4", "방명록을 수정했습니다.", managementService.updateMessage(user.userId, id, message))
    }

    @DeleteMapping("/api/guestbook/{id}")
    fun delete(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long): ApiResponse<Unit> {
        managementService.delete(user.userId, id)
        return ApiResponse.success("GUESTBOOK_200_5", "방명록을 삭제했습니다.")
    }
}
