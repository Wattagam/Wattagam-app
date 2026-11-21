package com.wattagam.common.security

data class AuthenticatedUser(val userId: Long, val needsNickname: Boolean)
