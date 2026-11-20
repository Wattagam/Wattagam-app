package com.wattagam.guestbook

import com.wattagam.common.api.ApiResponse
import com.wattagam.common.security.AuthenticatedUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GuestbookQueryController(private val queryService: GuestbookQueryService) {

    @GetMapping("/api/places/{id}/guestbook")
    fun getGallery(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "${GuestbookQueryService.DEFAULT_PAGE_SIZE}") size: Int,
    ): ApiResponse<GuestbookGalleryResponse> = ApiResponse.success(
        "GUESTBOOK_200_2",
        "방명록 갤러리를 조회했습니다.",
        queryService.getGallery(user.userId, id, page, size),
    )

    @GetMapping("/api/guestbook/{id}")
    fun getDetail(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long): ApiResponse<GuestbookDetailResponse> =
        ApiResponse.success("GUESTBOOK_200_3", "방명록을 조회했습니다.", queryService.getDetail(user.userId, id))

    @GetMapping("/api/me/guestbook")
    fun getMine(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "${GuestbookQueryService.DEFAULT_PAGE_SIZE}") size: Int,
    ): ApiResponse<GuestbookMyListResponse> = ApiResponse.success(
        "GUESTBOOK_200_6",
        "내 방명록을 조회했습니다.",
        queryService.getMine(user.userId, page, size),
    )
}
