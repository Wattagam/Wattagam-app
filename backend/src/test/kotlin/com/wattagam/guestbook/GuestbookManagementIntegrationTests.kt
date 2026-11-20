package com.wattagam.guestbook

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuestbookManagementIntegrationTests : GuestbookIntegrationTestSupport() {

    @Autowired
    private lateinit var cleanupService: GuestbookImageCleanupService

    private val base = Instant.parse("2026-11-06T08:00:00Z")

    @Test
    fun updatesOnlyTheMessageAndMarksTheEntryEdited() {
        val userId = createUser(101L, nickname = "푸앙이123")
        val entry = createEntryWithImage(userId, message = "처음", createdAt = base)

        patchMessage(entry.id!!, userId, """{"message":"메시지를 수정했어요"}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_200_4"))
            .andExpect(jsonPath("$.result.entryId").value(entry.id!!.toInt()))
            .andExpect(jsonPath("$.result.placeName").value("310관"))
            .andExpect(jsonPath("$.result.nickname").value("푸앙이123"))
            .andExpect(jsonPath("$.result.message").value("메시지를 수정했어요"))
            .andExpect(jsonPath("$.result.createdAt").value("2026-11-06T08:00:00Z"))
            .andExpect(jsonPath("$.result.edited").value(true))
            .andExpect(jsonPath("$.result.mine").value(true))

        val saved = guestbookEntryRepository.findById(entry.id!!).get()
        assertEquals("메시지를 수정했어요", saved.message)
        assertTrue(saved.updatedAt.isAfter(saved.createdAt))
        assertEquals(entry.imageObjectKey, saved.imageObjectKey)
        assertTrue(entry.imageObjectKey in fakeStorage.objects)
    }

    @Test
    fun clearsTheMessageWithNullOrAnEmptyString() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId, message = "지울 메시지")

        patchMessage(entry.id!!, userId, """{"message":null}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.message").value(null as String?))
        assertNull(guestbookEntryRepository.findById(entry.id!!).get().message)
        patchMessage(entry.id!!, userId, """{"message":"다시"}""").andExpect(status().isOk)
        patchMessage(entry.id!!, userId, """{"message":""}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.message").value(null as String?))
        assertNull(guestbookEntryRepository.findById(entry.id!!).get().message)
    }

    @Test
    fun rejectsInvalidUpdateBodiesWithoutChangingTheEntry() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId, message = "그대로")

        patchMessage(entry.id!!, userId, "{}").andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("COMMON_400_2"))
        patchMessage(entry.id!!, userId, """{"message":5}""").andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400_2"))
        patchMessage(entry.id!!, userId, "not json").andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400_2"))
        patchMessage(entry.id!!, userId, """{"message":"${"가".repeat(81)}"}""").andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_400_1"))
        patchMessage(0L, userId, """{"message":"x"}""").andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400_2"))

        val saved = guestbookEntryRepository.findById(entry.id!!).get()
        assertEquals("그대로", saved.message)
        assertEquals(saved.createdAt, saved.updatedAt)
    }

    @Test
    fun forbidsChangingAnotherUsersEntryAndReportsMissingOnes() {
        val ownerId = createUser(101L)
        val otherId = createUser(102L)
        val entry = createEntryWithImage(ownerId, message = "내 것")

        patchMessage(entry.id!!, otherId, """{"message":"탈취"}""")
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("COMMON_403_1"))
        deleteEntry(entry.id!!, otherId).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("COMMON_403_1"))
        patchMessage(99999L, ownerId, """{"message":"x"}""").andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_404_1"))
        deleteEntry(99999L, ownerId).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("GUESTBOOK_404_1"))

        assertEquals("내 것", guestbookEntryRepository.findById(entry.id!!).get().message)
        assertTrue(entry.imageObjectKey in fakeStorage.objects)
    }

    @Test
    fun deletesTheEntryAndItsImageButKeepsTheStamp() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId)
        grantStamp(userId, entry.id!!)

        deleteEntry(entry.id!!, userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_200_5"))
            .andExpect(jsonPath("$.result").value(null as String?))

        assertTrue(guestbookEntryRepository.findById(entry.id!!).isEmpty)
        assertTrue(fakeStorage.objects.isEmpty())
        assertTrue(cleanupRepository.findAll().isEmpty())
        val stamp = stampRepository.findAllByUserId(userId).single()
        assertNull(stamp.firstEntryId)
        getRequest("/api/me/stamps", userId)
            .andExpect(jsonPath("$.result.earnedCount").value(1))
            .andExpect(jsonPath("$.result.stamps[0].earned").value(true))
            .andExpect(jsonPath("$.result.stamps[0].firstEntryId").value(null as String?))
        getRequest("/api/guestbook/${entry.id}", userId).andExpect(status().isNotFound)
        deleteEntry(entry.id!!, userId).andExpect(status().isNotFound)
    }

    @Test
    fun answers200AndKeepsTheKeyForALaterRetryWhenTheImageCannotBeDeletedYet() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId)
        fakeStorage.failAllDeletes = true

        deleteEntry(entry.id!!, userId).andExpect(status().isOk)

        assertTrue(guestbookEntryRepository.findById(entry.id!!).isEmpty)
        assertEquals(listOf(entry.imageObjectKey), cleanupRepository.findAll().map { it.imageObjectKey })
        assertTrue(entry.imageObjectKey in fakeStorage.objects)

        cleanupService.retryPending()
        assertEquals(1, cleanupRepository.count())
        fakeStorage.failAllDeletes = false
        cleanupService.retryPending()

        assertTrue(cleanupRepository.findAll().isEmpty())
        assertTrue(fakeStorage.objects.isEmpty())
    }

    @Test
    fun deletesOnceWhenTwoDeleteRequestsRace() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures = List(2) {
                executor.submit<Int> {
                    ready.countDown()
                    check(start.await(10, TimeUnit.SECONDS))
                    deleteEntry(entry.id!!, userId).andReturn().response.status
                }
            }
            check(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            assertEquals(listOf(200, 404), futures.map { it.get(30, TimeUnit.SECONDS) }.sorted())
        } finally {
            executor.shutdownNow()
        }
        assertTrue(fakeStorage.objects.isEmpty())
        assertTrue(cleanupRepository.findAll().isEmpty())
    }

    @Test
    fun requiresAuthenticationAndANickname() {
        val entry = createEntryWithImage(createUser(101L))
        val nicknameless = createUser(102L, nickname = null)

        mockMvc.perform(patch("/api/guestbook/${entry.id}").contentType(MediaType.APPLICATION_JSON).content("""{"message":"x"}"""))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(delete("/api/guestbook/${entry.id}")).andExpect(status().isUnauthorized)
        patchMessage(entry.id!!, nicknameless, """{"message":"x"}""").andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("USER_403_1"))
        deleteEntry(entry.id!!, nicknameless).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("USER_403_1"))
    }

    private fun createEntryWithImage(userId: Long, message: String? = null, createdAt: Instant = Instant.now()): GuestbookEntry {
        val key = fakeStorage.storage.createKey()
        fakeStorage.storage.put(key, jpegBytes())
        return createEntry(userId, message = message, createdAt = createdAt, imageObjectKey = key)
    }

    private fun grantStamp(userId: Long, entryId: Long) {
        org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult {
            stampService.grantForPublishedEntry(userId, 1L, entryId)
        }
    }

    private fun patchMessage(entryId: Long, userId: Long, body: String): ResultActions = mockMvc.perform(
        patch("/api/guestbook/$entryId")
            .header(HttpHeaders.AUTHORIZATION, bearer(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
    )

    private fun deleteEntry(entryId: Long, userId: Long): ResultActions =
        mockMvc.perform(delete("/api/guestbook/$entryId").header(HttpHeaders.AUTHORIZATION, bearer(userId)))

    private fun getRequest(path: String, userId: Long): ResultActions =
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(userId)))
}
