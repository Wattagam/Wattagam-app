package com.wattagam.place

import com.wattagam.common.api.ApiResponse
import com.wattagam.common.security.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/places")
class PlaceController(
    private val placeService: PlaceService,
) {

    @GetMapping
    fun getPlaces(@AuthenticationPrincipal user: AuthenticatedUser): ApiResponse<PlaceListResponse> =
        ApiResponse.success("PLACE_200_1", "장소 목록을 조회했습니다.", placeService.getPlaces(user.userId))

    @GetMapping("/{id}")
    fun getPlace(@PathVariable id: Long): ApiResponse<PlaceDetailResponse> =
        ApiResponse.success("PLACE_200_2", "장소 정보를 조회했습니다.", placeService.getPlace(id))

    @PostMapping("/{id}/visits")
    fun checkVisit(
        @PathVariable id: Long,
        @Valid @RequestBody request: PlaceVisitRequest,
    ): ApiResponse<PlaceVisitResponse> =
        ApiResponse.success("PLACE_200_3", "현재 위치를 확인했습니다.", placeService.checkVisit(id, request))
}
