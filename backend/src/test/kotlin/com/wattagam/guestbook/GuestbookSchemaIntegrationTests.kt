package com.wattagam.guestbook

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuestbookSchemaIntegrationTests : GuestbookIntegrationTestSupport() {

    @Test
    fun keepsTheStampAndClearsFirstEntryIdWhenTheFirstEntryIsDeleted() {
        val userId = createUser(101L)
        val entry = createEntry(userId)
        grantStamp(userId, requireNotNull(entry.id))

        guestbookEntryRepository.deleteById(requireNotNull(entry.id))

        val stamp = stampRepository.findAllByUserId(userId).single()
        assertNull(stamp.firstEntryId)
    }

    @Test
    fun deletesEntriesAndStampsTogetherWithTheUser() {
        val userId = createUser(101L)
        val otherUserId = createUser(102L)
        grantStamp(userId, requireNotNull(createEntry(userId).id))
        val otherEntry = createEntry(otherUserId)
        grantStamp(otherUserId, requireNotNull(otherEntry.id))

        userRepository.deleteById(userId)

        assertTrue(guestbookEntryRepository.findAllByUserId(userId).isEmpty())
        assertTrue(stampRepository.findAllByUserId(userId).isEmpty())
        assertEquals(otherEntry.id, stampRepository.findAllByUserId(otherUserId).single().firstEntryId)
    }

    @Test
    fun rejectsTheSameClientRequestIdForTheSameUserOnly() {
        val userId = createUser(101L)
        val otherUserId = createUser(102L)
        val requestId = UUID.randomUUID().toString()
        guestbookEntryRepository.save(newEntry(userId, requestId))

        guestbookEntryRepository.save(newEntry(otherUserId, requestId))
        assertFailsWith<DataIntegrityViolationException> {
            guestbookEntryRepository.save(newEntry(userId, requestId))
        }
    }

    private fun newEntry(userId: Long, requestId: String): GuestbookEntry {
        val now = Instant.now()
        return GuestbookEntry(
            placeId = 1L,
            userId = userId,
            imageObjectKey = "guestbook/${UUID.randomUUID()}.jpg",
            message = null,
            clientRequestId = requestId,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun grantStamp(userId: Long, entryId: Long) {
        TransactionTemplate(transactionManager).executeWithoutResult {
            stampService.grantForPublishedEntry(userId, 1L, entryId)
        }
    }
}
