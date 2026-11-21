package com.wattagam.user

data class UserNicknameResponse(
    val userId: Long,
    val nickname: String,
    val needsNickname: Boolean = false,
)
