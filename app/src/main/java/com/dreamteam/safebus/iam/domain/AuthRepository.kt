package com.dreamteam.safebus.iam.domain

import com.dreamteam.safebus.shared.domain.DomainResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<Session?>
    suspend fun restore(): DomainResult<Session?>
    suspend fun signIn(loginId: String, password: String): DomainResult<Session>
    suspend fun signOut(): DomainResult<Unit>
}
