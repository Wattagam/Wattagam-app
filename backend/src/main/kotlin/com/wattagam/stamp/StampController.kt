package com.wattagam.stamp

import com.wattagam.common.api.ApiResponse
import com.wattagam.common.security.AuthenticatedUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me/stamps")
class StampController(private val stampService: StampService) {

    @GetMapping
    fun getBoard(@AuthenticationPrincipal user: AuthenticatedUser): ApiResponse<StampBoardResponse> =
        ApiResponse.success("STAMP_200_1", "도장판을 조회했습니다.", stampService.getBoard(user.userId))
}
