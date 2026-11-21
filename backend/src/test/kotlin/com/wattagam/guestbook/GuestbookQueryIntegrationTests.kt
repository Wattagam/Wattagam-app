package com.wattagam.guestbook

import java.time.Instant
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class GuestbookQueryIntegrationTests : GuestbookIntegrationTestSupport() {

    private val base = Instant.parse("2026-11-06T08:00:00Z")

    @Test
    fun listsAPlacesGalleryNewestFirstWithOwnershipAndCurrentNicknames() {
        val authorId = createUser(101L, nickname = "푸앙이123")
        val viewerId = createUser(102L, nickname = "캠퍼스러버")
        val older = createEntry(authorId, createdAt = base, message = "먼저")
        val newer = createEntry(viewerId, createdAt = base.plusSeconds(60), message = null)
        createEntry(authorId, placeId = 2L, createdAt = base.plusSeconds(120))
        val edited = createEntry(authorId, createdAt = base.plusSeconds(30))
        guestbookEntryRepository.save(edited.apply { updatedAt = base.plusSeconds(90) })

        request("/api/places/1/guestbook", viewerId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_200_2"))
            .andExpect(jsonPath("$.result.entries.length()").value(3))
            .andExpect(jsonPath("$.result.entries[0].entryId").value(newer.id!!.toInt()))
            .andExpect(jsonPath("$.result.entries[0].nickname").value("캠퍼스러버"))
            .andExpect(jsonPath("$.result.entries[0].imageUrl").value("/api/guestbook/${newer.id}/image"))
            .andExpect(jsonPath("$.result.entries[0].message").value(null as String?))
            .andExpect(jsonPath("$.result.entries[0].mine").value(true))
            .andExpect(jsonPath("$.result.entries[0].edited").value(false))
            .andExpect(jsonPath("$.result.entries[1].entryId").value(edited.id!!.toInt()))
            .andExpect(jsonPath("$.result.entries[1].edited").value(true))
            .andExpect(jsonPath("$.result.entries[1].mine").value(false))
            .andExpect(jsonPath("$.result.entries[2].entryId").value(older.id!!.toInt()))
            .andExpect(jsonPath("$.result.entries[2].nickname").value("푸앙이123"))
            .andExpect(jsonPath("$.result.page").value(0))
            .andExpect(jsonPath("$.result.size").value(20))
            .andExpect(jsonPath("$.result.hasNext").value(false))
    }

    @Test
    fun pagesTheGalleryAndBreaksTiesByIdDescending() {
        val userId = createUser(101L)
        val entries = List(3) { createEntry(userId, createdAt = base) }

        request("/api/places/1/guestbook?page=0&size=2", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.entries.length()").value(2))
            .andExpect(jsonPath("$.result.entries[0].entryId").value(entries[2].id!!.toInt()))
            .andExpect(jsonPath("$.result.entries[1].entryId").value(entries[1].id!!.toInt()))
            .andExpect(jsonPath("$.result.hasNext").value(true))
        request("/api/places/1/guestbook?page=1&size=2", userId)
            .andExpect(jsonPath("$.result.entries.length()").value(1))
            .andExpect(jsonPath("$.result.entries[0].entryId").value(entries[0].id!!.toInt()))
            .andExpect(jsonPath("$.result.hasNext").value(false))
        request("/api/places/1/guestbook?page=5&size=2", userId)
            .andExpect(jsonPath("$.result.entries.length()").value(0))
    }

    @Test
    fun answersAnEmptyGalleryWith200AndAnUnknownPlaceWith404() {
        val userId = createUser(101L)

        request("/api/places/3/guestbook", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.entries.length()").value(0))
            .andExpect(jsonPath("$.result.hasNext").value(false))
        request("/api/places/99/guestbook", userId)
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PLACE_404_1"))
    }

    @Test
    fun rejectsInvalidPagingAndIds() {
        val userId = createUser(101L)
        val badPaths = listOf(
            "/api/places/1/guestbook?page=-1",
            "/api/places/1/guestbook?size=0",
            "/api/places/1/guestbook?size=51",
            "/api/places/1/guestbook?page=abc",
            "/api/places/0/guestbook",
            "/api/guestbook/0",
            "/api/guestbook/abc",
            "/api/me/guestbook?size=51",
        )

        badPaths.forEach { path ->
            request(path, userId).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("COMMON_400_2"))
        }
        request("/api/places/1/guestbook?size=50", userId).andExpect(status().isOk)
    }

    @Test
    fun returnsTheDetailWithPlaceNameAndOwnership() {
        val authorId = createUser(101L, nickname = "푸앙이123")
        val viewerId = createUser(102L)
        val entry = createEntry(authorId, placeId = 2L, message = "도서관", createdAt = base)

        request("/api/guestbook/${entry.id}", authorId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_200_3"))
            .andExpect(jsonPath("$.result.entryId").value(entry.id!!.toInt()))
            .andExpect(jsonPath("$.result.placeId").value(2))
            .andExpect(jsonPath("$.result.placeName").value("도서관"))
            .andExpect(jsonPath("$.result.nickname").value("푸앙이123"))
            .andExpect(jsonPath("$.result.imageUrl").value("/api/guestbook/${entry.id}/image"))
            .andExpect(jsonPath("$.result.createdAt").value("2026-11-06T08:00:00Z"))
            .andExpect(jsonPath("$.result.updatedAt").value("2026-11-06T08:00:00Z"))
            .andExpect(jsonPath("$.result.edited").value(false))
            .andExpect(jsonPath("$.result.mine").value(true))
        request("/api/guestbook/${entry.id}", viewerId).andExpect(jsonPath("$.result.mine").value(false))
        request("/api/guestbook/99999", viewerId)
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_404_1"))
    }

    @Test
    fun listsOnlyMyEntriesNewestFirst() {
        val userId = createUser(101L)
        val otherId = createUser(102L)
        val first = createEntry(userId, placeId = 1L, createdAt = base)
        val second = createEntry(userId, placeId = 3L, createdAt = base.plusSeconds(10), message = "연못")
        createEntry(otherId, createdAt = base.plusSeconds(20))

        request("/api/me/guestbook", userId)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_200_6"))
            .andExpect(jsonPath("$.result.entries.length()").value(2))
            .andExpect(jsonPath("$.result.entries[0].entryId").value(second.id!!.toInt()))
            .andExpect(jsonPath("$.result.entries[0].placeName").value("청룡탕"))
            .andExpect(jsonPath("$.result.entries[0].message").value("연못"))
            .andExpect(jsonPath("$.result.entries[1].entryId").value(first.id!!.toInt()))
            .andExpect(jsonPath("$.result.entries[1].placeId").value(1))
            .andExpect(jsonPath("$.result.hasNext").value(false))
        request("/api/me/guestbook?size=1", userId).andExpect(jsonPath("$.result.hasNext").value(true))
        request("/api/me/guestbook", createUser(103L)).andExpect(jsonPath("$.result.entries.length()").value(0))
    }

    @Test
    fun requiresAuthenticationAndANicknameForEveryQuery() {
        val entry = createEntry(createUser(101L))
        val nicknameless = createUser(102L, nickname = null)

        listOf("/api/places/1/guestbook", "/api/guestbook/${entry.id}", "/api/me/guestbook").forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("COMMON_401_1"))
            request(path, nicknameless)
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("USER_403_1"))
        }
    }

    private fun request(path: String, userId: Long): ResultActions =
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(userId)))
}
