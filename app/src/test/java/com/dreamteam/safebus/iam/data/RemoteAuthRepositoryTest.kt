package com.dreamteam.safebus.iam.data

import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MemoryStore : SecureStore {
    val values = mutableMapOf<String, ByteArray>()
    var failWrites = false
    override fun read(key: String) = values[key]?.copyOf()
    override fun write(key: String, value: ByteArray) { check(!failWrites); values[key] = value.copyOf() }
    override fun remove(key: String) { values.remove(key) }
}

class RemoteAuthRepositoryTest {
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    private val store = MemoryStore()
    private val account = Session("1", Role.DRIVER, "private", now.plusSeconds(300), 1)
    private var answer: DomainResult<Session> = DomainResult.Success(account)
    private var logout: DomainResult<Unit> = DomainResult.Success(Unit)
    private val service = object : AuthService {
        override suspend fun signIn(loginId: String, password: String) = answer
        override suspend fun signOut() = logout
    }
    private fun repository(at: Instant = now) = RemoteAuthRepository(service, store, "https://example.com", Clock.fixed(at, ZoneOffset.UTC), Dispatchers.Unconfined)

    @Test fun storesOnlySuccessfulSessionsAndRestoresUntilExpiry() = runBlocking {
        val repo = repository()
        assertEquals(account, (repo.signIn("driver", "password") as DomainResult.Success).value)
        assertEquals(account, repo.session.value)
        assertEquals(account, (repository().restore() as DomainResult.Success).value)
        assertNull((repository(now.plusSeconds(300)).restore() as DomainResult.Success).value)
        assertTrue(store.values.isEmpty())
    }
    @Test fun invalidCredentialsAndStorageFailureNeverAuthenticateOrPersistInvalidData() = runBlocking {
        val repo = repository()
        for (failure in listOf(Failure.fromStatus(401), Failure(FailureKind.NETWORK))) {
            answer = DomainResult.Error(failure)
            assertEquals(answer, repo.signIn("driver", "wrong"))
            assertNull(repo.session.value)
            assertTrue(store.values.isEmpty())
        }
        answer = DomainResult.Success(account)
        store.failWrites = true
        assertEquals(FailureKind.STORAGE, (repo.signIn("driver", "valid") as DomainResult.Error).failure.kind)
        assertNull(repo.session.value)
        assertTrue(store.values.isEmpty())
    }
    @Test fun signsOutLocallyEvenOfflineAndIgnoresOldUnauthorizedCallbacks() = runBlocking {
        val repo = repository()
        repo.signIn("driver", "password")
        repo.invalidate("old-token")
        assertEquals(account, repo.session.value)
        logout = DomainResult.Error(Failure(FailureKind.NETWORK))
        assertEquals(logout, repo.signOut())
        assertNull(repo.session.value)
        assertTrue(store.values.isEmpty())
    }
}
