package com.wattagam.guestbook

import com.wattagam.stamp.StampService
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.util.AopTestUtils
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuestbookCreateIntegrationTests : GuestbookIntegrationTestSupport() {

    @MockitoSpyBean
    private lateinit var spiedStampService: StampService

    @Test
    fun createsTheEntryStoresTheImageAndGrantsTheFirstStamp() {
        val userId = createUser(101L)
        val image = jpegBytes()

        val response = postGuestbook(userId, image = image, message = "오늘도 여기 왔다감!")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.code").value("GUESTBOOK_201_1"))
            .andExpect(jsonPath("$.result.placeId").value(1))
            .andExpect(jsonPath("$.result.message").value("오늘도 여기 왔다감!"))
            .andExpect(jsonPath("$.result.stampAwarded").value(true))
            .andExpect(jsonPath("$.result.earnedCount").value(1))
            .andExpect(jsonPath("$.result.totalCount").value(3))

        val entry = guestbookEntryRepository.findAllByUserId(userId).single()
        response.andExpect(jsonPath("$.result.entryId").value(entry.id!!.toInt()))
            .andExpect(jsonPath("$.result.imageUrl").value("/api/guestbook/${entry.id}/image"))
            .andExpect(jsonPath("$.result.createdAt").value(entry.createdAt.toString()))
        assertContentEquals(image, fakeStorage.objects.getValue(entry.imageObjectKey))
        assertEquals(entry.id, stampRepository.findAllByUserId(userId).single().firstEntryId)
        assertEquals(entry.createdAt, entry.updatedAt)
    }

    @Test
    fun savesALaterEntryAtTheSamePlaceWithoutANewStamp() {
        val userId = createUser(101L)
        postGuestbook(userId).andExpect(status().isCreated)
        val firstStamp = stampRepository.findAllByUserId(userId).single()

        postGuestbook(userId, message = null)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.result.stampAwarded").value(false))
            .andExpect(jsonPath("$.result.earnedCount").value(1))

        assertEquals(2, guestbookEntryRepository.findAllByUserId(userId).size)
        assertEquals(firstStamp.firstEntryId, stampRepository.findAllByUserId(userId).single().firstEntryId)
        assertEquals(2, fakeStorage.objects.size)
    }

    @Test
    fun returnsTheExistingEntryForARetryWithTheSameClientRequestId() {
        val userId = createUser(101L)
        val requestId = UUID.randomUUID().toString()
        postGuestbook(userId, clientRequestId = requestId).andExpect(status().isCreated)
        val entry = guestbookEntryRepository.findAllByUserId(userId).single()

        postGuestbook(userId, clientRequestId = requestId, measuredAt = Instant.now().minusSeconds(600).toString())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.code").value("GUESTBOOK_200_1"))
            .andExpect(jsonPath("$.result.entryId").value(entry.id!!.toInt()))
            .andExpect(jsonPath("$.result.stampAwarded").value(false))
            .andExpect(jsonPath("$.result.earnedCount").value(1))

        assertEquals(1, guestbookEntryRepository.findAllByUserId(userId).size)
        assertEquals(1, fakeStorage.objects.size)
    }

    @Test
    fun rejectsReusingARequestIdForAnotherPlace() {
        val userId = createUser(101L)
        val requestId = UUID.randomUUID().toString()
        postGuestbook(userId, clientRequestId = requestId).andExpect(status().isCreated)

        postGuestbook(userId, placeId = 2L, clientRequestId = requestId, latitude = "37.504779", longitude = "126.957959")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400_2"))
    }

    @Test
    fun rejectsLocationsThatDoNotAllowWritingWithoutStoringAnything() {
        val userId = createUser(101L)
        val cases = listOf(
            Triple("GUESTBOOK_403_1", mapOf("latitude" to "37.510000"), 403),
            Triple("GUESTBOOK_403_2", mapOf("accuracyMeters" to "20.5"), 403),
            Triple("GUESTBOOK_403_3", mapOf("measuredAt" to Instant.now().minusSeconds(120).toString()), 403),
            Triple("COMMON_400_2", mapOf("measuredAt" to Instant.now().plusSeconds(120).toString()), 400),
            Triple("COMMON_400_2", mapOf("latitude" to "91"), 400),
        )

        cases.forEach { (code, overrides, httpStatus) ->
            postGuestbook(userId, overrides = overrides)
                .andExpect(status().`is`(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
        }

        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        assertTrue(fakeStorage.objects.isEmpty())
    }

    @Test
    fun validatesTheMessageLengthByCharacters() {
        val userId = createUser(101L)

        postGuestbook(userId, message = "가".repeat(80)).andExpect(status().isCreated)
        postGuestbook(userId, message = "😀".repeat(80)).andExpect(status().isCreated)
        postGuestbook(userId, message = "가".repeat(81))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_400_1"))

        assertEquals(2, guestbookEntryRepository.findAllByUserId(userId).size)
    }

    @Test
    fun rejectsInvalidImagesAndMissingFieldsWithoutStoringAnything() {
        val userId = createUser(101L)
        val badRequests = listOf(
            postGuestbook(userId, image = "not a jpeg".toByteArray()),
            postGuestbook(userId, image = ByteArray(5 * 1024 * 1024 + 1)),
            postGuestbook(userId, image = null),
            postGuestbook(userId, overrides = mapOf("clientRequestId" to "")),
            postGuestbook(userId, overrides = mapOf("clientRequestId" to "a".repeat(65))),
            postGuestbook(userId, overrides = mapOf("latitude" to "abc")),
            postGuestbook(userId, omit = setOf("accuracyMeters")),
            postGuestbook(userId, omit = setOf("measuredAt")),
        )

        badRequests.forEach { it.andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("COMMON_400_2")) }

        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(fakeStorage.objects.isEmpty())
    }

    @Test
    fun rejectsUnknownPlacesAndUnauthenticatedOrNicknamelessUsers() {
        val userId = createUser(101L)

        postGuestbook(userId, placeId = 99L).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("PLACE_404_1"))
        postGuestbook(userId, placeId = 0L).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("COMMON_400_2"))
        postGuestbook(null).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("COMMON_401_1"))
        postGuestbook(createUser(102L, nickname = null))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("USER_403_1"))

        assertTrue(fakeStorage.objects.isEmpty())
    }

    @Test
    fun retriesWithANewKeyWhenTheGeneratedKeyAlreadyExists() {
        val userId = createUser(101L)
        fakeStorage.rejectNextPuts = 1

        postGuestbook(userId).andExpect(status().isCreated)

        val entry = guestbookEntryRepository.findAllByUserId(userId).single()
        assertEquals(setOf(entry.imageObjectKey), fakeStorage.objects.keys)
        assertTrue(fakeStorage.deletedKeys.isEmpty())
    }

    @Test
    fun createsNothingWhenTheImageCannotBeStored() {
        val userId = createUser(101L)
        fakeStorage.failPut = true

        postGuestbook(userId)
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_503_1"))

        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun removesTheStoredImageAndRollsBackWhenTheStampGrantFails() {
        val userId = createUser(101L)
        // 트랜잭션 프록시를 거치면 스텁 등록 자체가 MANDATORY 검사에 걸리므로 대상 객체에 직접 건다.
        val stampTarget = AopTestUtils.getUltimateTargetObject<StampService>(spiedStampService)
        Mockito.doThrow(IllegalStateException("도장 지급 실패")).`when`(stampTarget)
            .grantForPublishedEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())

        postGuestbook(userId).andExpect(status().isInternalServerError)

        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        assertTrue(fakeStorage.objects.isEmpty())
        assertEquals(1, fakeStorage.deletedKeys.size)
    }

    @Test
    fun keepsTheKeyForALaterRetryWhenCleaningUpAfterAFailureAlsoFails() {
        val userId = createUser(101L)
        val stampTarget = AopTestUtils.getUltimateTargetObject<StampService>(spiedStampService)
        Mockito.doThrow(IllegalStateException("도장 지급 실패")).`when`(stampTarget)
            .grantForPublishedEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())
        fakeStorage.failAllDeletes = true

        postGuestbook(userId).andExpect(status().isInternalServerError)

        val storedKey = fakeStorage.objects.keys.single()
        assertEquals(listOf(storedKey), cleanupRepository.findAll().map { it.imageObjectKey })
        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun savesOneEntryAndOneStampWhenTheSameRequestRacesItself() {
        val userId = createUser(101L)
        val requestId = UUID.randomUUID().toString()

        val statuses = runConcurrently(2) { postGuestbook(userId, clientRequestId = requestId).andReturn().response.status }

        assertEquals(listOf(200, 201), statuses.sorted())
        assertEquals(1, guestbookEntryRepository.findAllByUserId(userId).size)
        assertEquals(1, stampRepository.findAllByUserId(userId).size)
        assertEquals(1, fakeStorage.objects.size)
    }

    @Test
    fun grantsExactlyOneStampWhenDifferentRequestsRaceForTheSamePlace() {
        val userId = createUser(101L)

        val bodies = runConcurrently(3) { postGuestbook(userId).andReturn().response.contentAsString }

        assertEquals(3, guestbookEntryRepository.findAllByUserId(userId).size)
        assertEquals(1, stampRepository.findAllByUserId(userId).size)
        assertEquals(1, bodies.count { it.contains("\"stampAwarded\":true") })
        assertEquals(3, fakeStorage.objects.size)
    }

    private fun <T> runConcurrently(count: Int, action: () -> T): List<T> {
        val ready = CountDownLatch(count)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(count)
        try {
            val futures = List(count) {
                executor.submit<T> {
                    ready.countDown()
                    check(start.await(10, TimeUnit.SECONDS))
                    action()
                }
            }
            check(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    // 기본값은 310관 중심 좌표와 방금 측정한 위치다. userId가 null이면 인증 없이 요청한다.
    private fun postGuestbook(
        userId: Long?,
        placeId: Long = 1L,
        image: ByteArray? = jpegBytes(),
        message: String? = "안녕",
        clientRequestId: String = UUID.randomUUID().toString(),
        measuredAt: String = Instant.now().toString(),
        latitude: String = "37.503645",
        longitude: String = "126.956032",
        overrides: Map<String, String> = emptyMap(),
        omit: Set<String> = emptySet(),
    ): ResultActions {
        val fields = linkedMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracyMeters" to "8",
            "measuredAt" to measuredAt,
            "clientRequestId" to clientRequestId,
        ).apply { message?.let { put("message", it) } }.apply { putAll(overrides) }
        val request = multipart("/api/places/$placeId/guestbook")
        image?.let { request.file(MockMultipartFile("image", "photo.jpg", "image/jpeg", it)) }
        fields.filterKeys { it !in omit }.forEach { (name, value) -> request.param(name, value) }
        userId?.let { request.header(HttpHeaders.AUTHORIZATION, bearer(it)) }
        return mockMvc.perform(request)
    }
}
