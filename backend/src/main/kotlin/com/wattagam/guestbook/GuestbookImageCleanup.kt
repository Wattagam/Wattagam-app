package com.wattagam.guestbook

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/** 방명록은 삭제됐지만 OCI 삭제가 끝나지 않아 재시도해야 하는 이미지 객체 키. */
@Entity
@Table(
    name = "guestbook_image_cleanups",
    uniqueConstraints = [UniqueConstraint(name = "uk_guestbook_image_cleanups_key", columnNames = ["image_object_key"])],
)
class GuestbookImageCleanup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "image_object_key", nullable = false, updatable = false, length = 255)
    val imageObjectKey: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)
