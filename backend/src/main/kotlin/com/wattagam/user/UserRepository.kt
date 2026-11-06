package com.wattagam.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByKakaoUserId(kakaoUserId: Long): User?
}
