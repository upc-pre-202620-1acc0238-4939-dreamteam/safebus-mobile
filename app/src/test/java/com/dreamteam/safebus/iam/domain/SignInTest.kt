package com.dreamteam.safebus.iam.domain

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SignInTest {
    @Test fun rejectsEmptyCredentialsWithoutCallingRepositoryAndPreservesPassword() = runBlocking {
        var received: Pair<String, String>? = null
        val repo = object : AuthRepository {
            override val session = MutableStateFlow<Session?>(null)
            override suspend fun restore() = DomainResult.Success(null)
            override suspend fun signIn(loginId: String, password: String): DomainResult<Session> {
                received = loginId to password
                return DomainResult.Error(Failure.fromStatus(401))
            }
            override suspend fun signOut() = DomainResult.Success(Unit)
        }
        val action = SignIn(repo)
        assertTrue(action(" ", "password") is DomainResult.Error)
        assertNull(received)
        assertNull(repo.session.value)
        assertTrue(action(" driver ", " pass ") is DomainResult.Error)
        assertEquals("driver" to " pass ", received)
        assertNull(repo.session.value)
    }
}
