package com.dreamteam.safebus.iam.domain

import java.time.Instant

enum class Role { DRIVER, PASSENGER, SUPERVISOR }

data class Session(val userId: String, val role: Role, val accessToken: String, val expiresAt: Instant, val companyId: Long?) {
    init {
        require(userId.toLongOrNull()?.let { it > 0 } == true)
        require(accessToken.isNotBlank())
        require(role == Role.PASSENGER || companyId?.let { it > 0 } == true)
    }
    fun validAt(now: Instant): Boolean = expiresAt.isAfter(now)
    override fun toString(): String = "Session(role=$role, expiresAt=$expiresAt)"
}
