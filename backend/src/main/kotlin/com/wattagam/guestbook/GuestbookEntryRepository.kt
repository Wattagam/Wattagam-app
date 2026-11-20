package com.wattagam.guestbook

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GuestbookEntryRepository : JpaRepository<GuestbookEntry, Long> {

    fun findByUserIdAndClientRequestId(userId: Long, clientRequestId: String): GuestbookEntry?

    fun findAllByUserId(userId: Long): List<GuestbookEntry>

    fun existsByImageObjectKey(imageObjectKey: String): Boolean

    @Query(
        """
        select new com.wattagam.guestbook.GuestbookRow(
            e.id, e.placeId, p.name, e.userId, u.nickname, e.message, e.createdAt, e.updatedAt)
        from GuestbookEntry e, Place p, User u
        where p.id = e.placeId and u.id = e.userId and e.placeId = :placeId
        order by e.createdAt desc, e.id desc
        """,
    )
    fun findGalleryRows(@Param("placeId") placeId: Long, pageable: Pageable): Slice<GuestbookRow>

    @Query(
        """
        select new com.wattagam.guestbook.GuestbookRow(
            e.id, e.placeId, p.name, e.userId, u.nickname, e.message, e.createdAt, e.updatedAt)
        from GuestbookEntry e, Place p, User u
        where p.id = e.placeId and u.id = e.userId and e.userId = :userId
        order by e.createdAt desc, e.id desc
        """,
    )
    fun findRowsByAuthor(@Param("userId") userId: Long, pageable: Pageable): Slice<GuestbookRow>

    @Query(
        """
        select new com.wattagam.guestbook.GuestbookRow(
            e.id, e.placeId, p.name, e.userId, u.nickname, e.message, e.createdAt, e.updatedAt)
        from GuestbookEntry e, Place p, User u
        where p.id = e.placeId and u.id = e.userId and e.id = :entryId
        """,
    )
    fun findDetailRow(@Param("entryId") entryId: Long): GuestbookRow?
}
