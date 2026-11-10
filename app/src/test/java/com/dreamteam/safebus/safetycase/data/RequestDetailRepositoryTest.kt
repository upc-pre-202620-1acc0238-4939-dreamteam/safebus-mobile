package com.dreamteam.safebus.safetycase.data

import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RequestDetailRepositoryTest {
    @Test fun invalidAndForeignRequestsNeverReadOrWritePrivateLocalRecords() = runBlocking {
        var calls = 0
        var writes = 0
        val store = object : SecureStore {
            override fun read(key: String): ByteArray? = null
            override fun write(key: String, value: ByteArray) { writes++ }
            override fun remove(key: String) { writes++ }
        }
        val api = ApiClient("https://example.com", false, { "token" }, {}, HttpEngine {
            calls++; ApiResponse(403, byteArrayOf())
        }, Dispatchers.Unconfined, { "PASSENGER_REQUEST_ACCESS_DENIED" })
        val repo = RemoteSafetyRepository(api, Outbox(store, "test", PendingTransport { DomainResult.Success(Unit) }), store, "test")
        assertTrue(repo.detail("photo") is DomainResult.Error); assertEquals(0, calls); assertEquals(0, writes)
        assertEquals(FailureKind.FORBIDDEN, (repo.detail(UUID.randomUUID().toString()) as DomainResult.Error).failure.kind)
        assertEquals(1, calls); assertEquals(0, writes)
    }
}
