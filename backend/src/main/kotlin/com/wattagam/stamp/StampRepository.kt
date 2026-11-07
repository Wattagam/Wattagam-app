package com.wattagam.stamp

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StampRepository : JpaRepository<Stamp, Long> {

    fun findAllByUserId(userId: Long): List<Stamp>

    // 중복 지급 요청에서도 최초 도장과 호출자의 방명록 트랜잭션을 보존한다.
    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO stamps (user_id, place_id, first_entry_id, earned_at)
            VALUES (:userId, :placeId, :entryId, :earnedAt)
            ON DUPLICATE KEY UPDATE id = id
        """,
        nativeQuery = true,
    )
    fun insertIfMissing(
        @Param("userId") userId: Long,
        @Param("placeId") placeId: Long,
        @Param("entryId") entryId: Long,
        @Param("earnedAt") earnedAt: Instant,
    )
}
