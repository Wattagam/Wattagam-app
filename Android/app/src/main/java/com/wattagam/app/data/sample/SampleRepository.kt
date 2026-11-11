package com.wattagam.app.data.sample

import com.wattagam.app.data.Account
import com.wattagam.app.data.CampusPlace
import com.wattagam.app.data.CampusPlaceDetail
import com.wattagam.app.data.GuestbookEntry
import com.wattagam.app.data.GuestbookPage
import com.wattagam.app.data.PublishOutcome
import com.wattagam.app.data.StampBoardData
import com.wattagam.app.data.StampSlot
import com.wattagam.app.data.WattagamRepository
import com.wattagam.app.data.api.ApiException
import com.wattagam.app.data.auth.KakaoLogin
import com.wattagam.app.data.auth.LocalProfile
import com.wattagam.app.data.location.LocationFix
import com.wattagam.app.data.location.VisitEligibility
import com.wattagam.app.data.location.VisitRule
import java.io.File

/**
 * 서버 주소가 없을 때 쓰는 샘플 저장소. 장소·기록·도장은 [SampleData] 이고 등록은 하지 않는다.
 * 카카오 로그인(앱 키가 있을 때)과 GPS 판정(앱 쪽 30 m 기준)은 실제로 한다. 닉네임은 이 기기에만 저장한다.
 */
class SampleRepository(private val profile: LocalProfile) : WattagamRepository {
    override val connectedToServer = false
    override val courseName = SampleData.COURSE

    /** 샘플 기록 중 '내 기록'에 붙일 이름 */
    @Volatile private var nickname: String = SampleData.NICKNAME

    override suspend fun restoreSession(): Account? =
        if (KakaoLogin.isConfigured && KakaoLogin.hasSession()) account() else null

    override suspend fun signIn(kakaoAccessToken: String?): Account = account()

    override suspend fun saveNickname(nickname: String): Account {
        profile.saveNickname(nickname)
        this.nickname = nickname
        return Account(nickname)
    }

    override suspend fun logout() = profile.clear()

    /** 탈퇴 API 가 없는 샘플: 이 기기의 닉네임만 지운다 */
    override suspend fun withdraw() = profile.clear()

    override suspend fun places(): List<CampusPlace> = SampleData.places.map { it.toPlace() }

    override fun cachedPlace(placeId: String): CampusPlace? = SampleData.place(placeId)?.toPlace()

    override suspend fun placeDetail(placeId: String): CampusPlaceDetail {
        val place = SampleData.place(placeId) ?: throw ApiException(404, "PLACE_404_1", null)
        return CampusPlaceDetail(
            place.toPlace(), place.description, place.highlight, place.writingPrompt, place.directions,
            publicCount = SampleData.guestbooksAt(placeId).size,
        )
    }

    override fun cachedStampBoard(): StampBoardData = StampBoardData(
        SampleData.places.count { it.stamped }, SampleData.places.size,
        SampleData.places.map { StampSlot(it.id, it.name, it.stamped) },
    )

    override suspend fun stampBoard(): StampBoardData = cachedStampBoard()

    override suspend fun checkVisit(placeId: String, fix: LocationFix.Found): VisitEligibility {
        val place = SampleData.place(placeId) ?: throw ApiException(404, "PLACE_404_1", null)
        return VisitRule.evaluate(fix, place.latitude, place.longitude)
    }

    override suspend fun gallery(placeId: String, page: Int): GuestbookPage =
        GuestbookPage(if (page == 0) SampleData.guestbooksAt(placeId).map { it.toEntry() } else emptyList(), hasNext = false)

    override suspend fun guestbook(entryId: String): GuestbookEntry =
        SampleData.guestbook(entryId)?.toEntry() ?: throw ApiException(404, "GUESTBOOK_404_1", null)

    override suspend fun myGuestbooks(): GuestbookPage = GuestbookPage(SampleData.myGuestbooks().map { it.toEntry() }, hasNext = false)

    override suspend fun publish(placeId: String, photo: File, message: String, fix: LocationFix.Found, requestId: String) =
        PublishOutcome.NotConnected

    private suspend fun account(): Account {
        val saved = profile.nickname()
        if (saved != null) nickname = saved
        return Account(saved)
    }

    private fun SamplePlace.toPlace() = CampusPlace(id, order, name, checkpoint, latitude, longitude, stamped)

    private fun SampleGuestbook.toEntry() = GuestbookEntry(
        id, placeId, SampleData.place(placeId)?.name.orEmpty(), if (mine) nickname else author, title, message,
        imageUrl = null, createdAt, mine,
    )
}
