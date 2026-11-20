package com.wattagam.guestbook

import com.wattagam.user.WithdrawalContentCleanup
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuestbookWithdrawalIntegrationTests : GuestbookIntegrationTestSupport() {

    @Autowired
    private lateinit var contentCleanup: WithdrawalContentCleanup

    @Test
    fun withdrawsTheUserWithTheirEntriesImagesAndStampsAndKeepsOthers() {
        val userId = createUser(101L)
        val otherId = createUser(102L)
        val mine = List(2) { createEntryWithImage(userId) }
        val others = createEntryWithImage(otherId)
        grantStamp(userId, requireNotNull(mine[0].id))
        grantStamp(otherId, requireNotNull(others.id))
        val token = jwtService.issueAccessToken(userId)

        withdraw(token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("USER_200_2"))

        assertFalse(userRepository.existsById(userId))
        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        assertEquals(setOf(others.imageObjectKey), fakeStorage.objects.keys)
        assertEquals(others.id, stampRepository.findAllByUserId(otherId).single().firstEntryId)
        mockMvc.perform(get("/api/me/guestbook").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/guestbook/${others.id}").header(HttpHeaders.AUTHORIZATION, bearer(otherId)))
            .andExpect(status().isOk)
    }

    @Test
    fun keepsTheAccountAndFinishesOnRetryAfterSomeImagesWereAlreadyDeleted() {
        val userId = createUser(101L)
        val entries = List(3) { createEntryWithImage(userId) }
        grantStamp(userId, requireNotNull(entries[0].id))
        val stuckKey = entries[1].imageObjectKey
        fakeStorage.failDeleteKeys = setOf(stuckKey)
        val token = jwtService.issueAccessToken(userId)

        withdraw(token)
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("USER_503_1"))

        assertTrue(userRepository.existsById(userId))
        assertEquals(3, guestbookEntryRepository.findAllByUserId(userId).size)
        assertEquals(1, stampRepository.findAllByUserId(userId).size)
        assertEquals(setOf(stuckKey), fakeStorage.objects.keys)

        fakeStorage.failDeleteKeys = emptySet()
        withdraw(token).andExpect(status().isOk)

        assertFalse(userRepository.existsById(userId))
        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(fakeStorage.objects.isEmpty())
    }

    @Test
    fun reportsFailureWithoutDeletingRowsAndTreatsMissingImagesAsDone() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId)
        createEntry(userId)

        fakeStorage.failAllDeletes = true
        assertFalse(contentCleanup.deleteForUser(userId))
        assertEquals(2, guestbookEntryRepository.findAllByUserId(userId).size)

        fakeStorage.failAllDeletes = false
        fakeStorage.objects.remove(entry.imageObjectKey)
        assertTrue(contentCleanup.deleteForUser(userId))
        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun withdrawsAUserWithoutEntriesEvenWhenTheStorageIsDown() {
        val userId = createUser(101L)
        fakeStorage.failAllDeletes = true

        withdraw(jwtService.issueAccessToken(userId)).andExpect(status().isOk)

        assertFalse(userRepository.existsById(userId))
    }

    @Test
    fun leavesNoEntriesOrImagesBehindWhenAPublicationRacesWithdrawal() {
        repeat(3) { round ->
            val userId = createUser(200L + round)
            val token = jwtService.issueAccessToken(userId)
            val ready = CountDownLatch(2)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val tasks = listOf<() -> Int>(
                    { createGuestbook(token).andReturn().response.status },
                    { withdraw(token).andReturn().response.status },
                ).map { task ->
                    executor.submit<Int> {
                        ready.countDown()
                        check(start.await(10, TimeUnit.SECONDS))
                        task()
                    }
                }
                check(ready.await(10, TimeUnit.SECONDS))
                start.countDown()
                tasks.forEach { it.get(30, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }

            assertFalse(userRepository.existsById(userId))
            assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
            assertTrue(fakeStorage.objects.isEmpty())
        }
    }

    private fun createEntryWithImage(userId: Long): GuestbookEntry {
        val key = fakeStorage.storage.createKey()
        fakeStorage.storage.put(key, jpegBytes())
        return createEntry(userId, imageObjectKey = key)
    }

    private fun grantStamp(userId: Long, entryId: Long) {
        TransactionTemplate(transactionManager).executeWithoutResult {
            stampService.grantForPublishedEntry(userId, 1L, entryId)
        }
    }

    private fun withdraw(token: String): ResultActions =
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))

    private fun createGuestbook(token: String): ResultActions = mockMvc.perform(
        multipart("/api/places/1/guestbook")
            .file(MockMultipartFile("image", "photo.jpg", "image/jpeg", jpegBytes()))
            .param("latitude", "37.503645")
            .param("longitude", "126.956032")
            .param("accuracyMeters", "8")
            .param("measuredAt", Instant.now().toString())
            .param("clientRequestId", UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    )
}
