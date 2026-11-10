package com.dreamteam.safebus.iam.data

import com.dreamteam.safebus.iam.domain.AuthRepository
import com.dreamteam.safebus.iam.domain.Session
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.util.Base64

interface AuthService {
    suspend fun signIn(loginId: String, password: String): DomainResult<Session>
    suspend fun signOut(): DomainResult<Unit>
}

class HttpAuthService(private val api: ApiClient, private val clock: Clock = Clock.systemUTC()) : AuthService {
    override suspend fun signIn(loginId: String, password: String): DomainResult<Session> = api.call("POST", "/api/v1/auth/sign-in",
        jsonBody("loginId" to loginId, "password" to password), "application/json", authenticated = false) { response ->
        val body = response.json()
        val dto = SessionDto(body.getString("accessToken"), body.getString("tokenType"), body.getString("role"), body.getString("expiresAt"))
        val parts = dto.accessToken.split('.')
        require(parts.size == 3)
        val claims = JSONObject(Base64.getUrlDecoder().decode(parts[1]).toString(Charsets.UTF_8))
        dto.toDomain(claims.getString("sub"), claims.getString("role"), claims.optionalLong("companyId"), Instant.ofEpochSecond(claims.getLong("exp")), clock.instant())
    }
    override suspend fun signOut(): DomainResult<Unit> = api.call("POST", "/api/v1/auth/sign-out") {}
}

class RemoteAuthRepository(private val service: AuthService, private val store: SecureStore, private val endpoint: String,
    private val clock: Clock = Clock.systemUTC(), private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : AuthRepository {
    private val mutableSession = MutableStateFlow<Session?>(null)
    override val session = mutableSession.asStateFlow()
    private val key = "session:$endpoint"

    override suspend fun restore(): DomainResult<Session?> = withContext(dispatcher) {
        try {
            val saved = store.read(key)?.let(SessionRecordCodec::decode)
            val valid = saved?.takeIf { it.validAt(clock.instant()) }
            if (saved != null && valid == null) store.remove(key)
            mutableSession.value = valid
            DomainResult.Success(valid)
        } catch (_: Exception) {
            mutableSession.value = null
            runCatching { store.remove(key) }
            DomainResult.Error(Failure(FailureKind.STORAGE))
        }
    }
    override suspend fun signIn(loginId: String, password: String): DomainResult<Session> = withContext(dispatcher) {
        when (val result = service.signIn(loginId, password)) {
            is DomainResult.Error -> result
            is DomainResult.Success -> try {
                store.write(key, SessionRecordCodec.encode(result.value))
                mutableSession.value = result.value
                result
            } catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
        }
    }
    override suspend fun signOut(): DomainResult<Unit> = withContext(dispatcher) {
        val result = try { service.signOut() } finally { invalidate(mutableSession.value?.accessToken) }
        result
    }
    @Synchronized fun invalidate(token: String?) {
        if (mutableSession.value?.accessToken == token) {
            mutableSession.value = null
            store.remove(key)
        }
    }
    fun token(): String? {
        val current = mutableSession.value ?: return null
        if (!current.validAt(clock.instant())) { invalidate(current.accessToken); return null }
        return current.accessToken
    }
}
