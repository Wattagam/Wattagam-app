package com.wattagam.stamp

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "stamps",
    uniqueConstraints = [UniqueConstraint(name = "uk_stamps_user_place", columnNames = ["user_id", "place_id"])],
)
class Stamp(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,
    @Column(name = "place_id", nullable = false, updatable = false)
    val placeId: Long,
    @Column(name = "first_entry_id", updatable = false)
    val firstEntryId: Long?,
    @Column(name = "earned_at", nullable = false, updatable = false)
    val earnedAt: Instant,
)
