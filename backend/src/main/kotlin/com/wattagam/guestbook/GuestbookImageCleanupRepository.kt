package com.wattagam.guestbook

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface GuestbookImageCleanupRepository : JpaRepository<GuestbookImageCleanup, Long> {

    fun findTop50ByOrderByIdAsc(): List<GuestbookImageCleanup>

    fun existsByImageObjectKey(imageObjectKey: String): Boolean

    @Transactional
    @Modifying
    @Query("delete from GuestbookImageCleanup c where c.imageObjectKey = :imageObjectKey")
    fun deleteByKey(@Param("imageObjectKey") imageObjectKey: String)
}
