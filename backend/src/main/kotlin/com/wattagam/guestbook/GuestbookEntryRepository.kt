package com.wattagam.guestbook

import org.springframework.data.jpa.repository.JpaRepository

interface GuestbookEntryRepository : JpaRepository<GuestbookEntry, Long> {

    fun findByUserIdAndClientRequestId(userId: Long, clientRequestId: String): GuestbookEntry?

    fun findAllByUserId(userId: Long): List<GuestbookEntry>
}
