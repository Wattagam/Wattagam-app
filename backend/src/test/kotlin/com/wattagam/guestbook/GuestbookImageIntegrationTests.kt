package com.wattagam.guestbook

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertContentEquals

class GuestbookImageIntegrationTests : GuestbookIntegrationTestSupport() {

    @Test
    fun returnsTheJpegBytesToAnyAuthenticatedUserWithoutCaching() {
        val authorId = createUser(101L)
        val viewerId = createUser(102L)
        val image = jpegBytes()
        val entry = createEntryWithImage(authorId, image)

        val response = requestImage(entry.id!!, viewerId)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store, private"))
            .andReturn().response

        assertContentEquals(image, response.contentAsByteArray)
    }

    @Test
    fun answersMissingEntriesAndObjectsWithAJsonNotFoundEvenForImageLoaders() {
        val userId = createUser(101L)
        val entry = createEntry(userId)

        requestImage(99999L, userId, accept = "image/*")
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("GUESTBOOK_404_1"))
        requestImage(entry.id!!, userId, accept = "image/*")
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_404_1"))
        requestImage(0L, userId)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400_2"))
    }

    @Test
    fun requiresAuthenticationAndANickname() {
        val entry = createEntryWithImage(createUser(101L), jpegBytes())

        mockMvc.perform(get("/api/guestbook/${entry.id}/image"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("COMMON_401_1"))
        requestImage(entry.id!!, createUser(102L, nickname = null))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("USER_403_1"))
    }

    @Test
    fun answers503WhenTheStorageFailsWithoutExposingItsDetails() {
        val userId = createUser(101L)
        val entry = createEntryWithImage(userId, jpegBytes())
        fakeStorage.failGet = true

        requestImage(entry.id!!, userId, accept = "image/*")
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("GUESTBOOK_503_1"))
            .andExpect(jsonPath("$.message").value("이미지를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."))
    }

    private fun createEntryWithImage(userId: Long, image: ByteArray): GuestbookEntry {
        val key = fakeStorage.storage.createKey()
        fakeStorage.storage.put(key, image)
        return createEntry(userId, imageObjectKey = key)
    }

    private fun requestImage(entryId: Long, userId: Long, accept: String = "*/*"): ResultActions =
        mockMvc.perform(
            get("/api/guestbook/$entryId/image")
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .header(HttpHeaders.ACCEPT, accept),
        )
}
