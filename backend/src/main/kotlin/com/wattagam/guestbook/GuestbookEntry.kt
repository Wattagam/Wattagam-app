package com.wattagam.guestbook

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
    name = "guestbook_entries",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_guestbook_entries_request", columnNames = ["user_id", "client_request_id"]),
        UniqueConstraint(name = "uk_guestbook_entries_image_key", columnNames = ["image_object_key"]),
    ],
)
class GuestbookEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "place_id", nullable = false, updatable = false)
    val placeId: Long,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,
    // 이미지 조회·정리에만 쓰는 OCI 객체 키. 응답과 로그에 노출하지 않는다.
    @Column(name = "image_object_key", nullable = false, updatable = false, length = 255)
    val imageObjectKey: String,
    @Column(name = "message", length = 80)
    var message: String?,
    @Column(name = "client_request_id", nullable = false, updatable = false, length = 64)
    val clientRequestId: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
