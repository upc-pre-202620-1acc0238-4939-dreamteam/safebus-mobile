package com.dreamteam.safebus.iam.ui

import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class AuthViewModelTest {
    @Test fun reflectsAuthenticationErrorsAndRemovesSessionAfterOfflineLogout() {
        val session = MutableStateFlow<Session?>(null)
        var result: DomainResult<Session> = DomainResult.Error(Failure.fromStatus(401))
        val repository = object : AuthRepository {
            override val session = session
            override suspend fun restore() = DomainResult.Success(session.value)
            override suspend fun signIn(loginId: String, password: String): DomainResult<Session> {
                if (result is DomainResult.Success) session.value = (result as DomainResult.Success).value
                return result
            }
            override suspend fun signOut(): DomainResult<Unit> { session.value = null; return DomainResult.Error(Failure(FailureKind.NETWORK)) }
        }
        val model = AuthViewModel(repository, { DomainResult.Success(1) }, dispatcher = Dispatchers.Unconfined)
        model.signIn("login", "bad")
        assertEquals(FailureKind.UNAUTHORIZED, model.state.value.failure?.kind)
        assertNull(model.state.value.value.session); assertNull(session.value)
        result = DomainResult.Success(Session("1", Role.PASSENGER, "token", Instant.now().plusSeconds(100), null))
        model.signIn("login", "valid")
        assertEquals(Role.PASSENGER, model.state.value.value.session?.role)
        assertNull(model.state.value.failure)
        model.signOut()
        assertEquals(FailureKind.NETWORK, model.state.value.failure?.kind)
        assertNull(model.state.value.value.session); assertNull(session.value)
    }
}
