package com.wattagam.user

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
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_kakao_user_id", columnNames = ["kakao_user_id"])],
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "kakao_user_id", nullable = false, updatable = false)
    val kakaoUserId: Long,
    @Column(name = "nickname", length = 10)
    var nickname: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)
