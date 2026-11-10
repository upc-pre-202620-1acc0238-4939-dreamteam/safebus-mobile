package com.dreamteam.safebus.safetycase.data

import com.dreamteam.safebus.iam.data.MemoryStore
import com.dreamteam.safebus.safetycase.domain.SubmitRequest
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RemoteSafetyRepositoryTest {
    @Test fun rejectedDriverAlertIsNotMisrepresentedAsPendingOrKeptLocally() = runBlocking {
        val store = MemoryStore()
        store.write("driver-alerts:owner", "00000000-0000-0000-0000-000000000001|2026-11-10T12:00:00Z".toByteArray())
        val api = ApiClient("https://example.com", false, { "token" }, {}, HttpEngine { ApiResponse(404, byteArrayOf()) }, Dispatchers.Unconfined, { "" })
        val outbox = Outbox(store, "owner", PendingTransport { DomainResult.Success(Unit) }, Dispatchers.Unconfined)
        val repo = RemoteSafetyRepository(api, outbox, store, "owner", dispatcher = Dispatchers.Unconfined)
        assertEquals("ALERT_NOT_DELIVERED", (repo.driverAlerts() as DomainResult.Error).failure.code)
        assertTrue(store.values.isEmpty())
    }
    @Test fun rejectsInvalidActionsBeforeAnyNetworkOrLocalWrite() = runBlocking {
        val store = MemoryStore()
        val api = ApiClient("https://example.com", false, { "token" }, {}, HttpEngine { error("Unexpected network") }, Dispatchers.Unconfined)
        val outbox = Outbox(store, "owner", PendingTransport { error("Unexpected delivery") }, Dispatchers.Unconfined)
        val repo = RemoteSafetyRepository(api, outbox, store, "owner", dispatcher = Dispatchers.Unconfined)
        val results = listOf(repo.panic(0), repo.request(0, "message", Photo(byteArrayOf(1), "image/png")),
            repo.approve(0), repo.decline(1, " ", "response"), repo.close("bad", "outcome", ""), repo.attend("bad"), repo.evidence("../1", false),
            SubmitRequest(repo)(null, "message", null), SubmitRequest(repo)(1, "a".repeat(501), Photo(byteArrayOf(1), "image/png")))
        results.forEach { assertEquals(FailureKind.VALIDATION, (it as DomainResult.Error).failure.kind); assertTrue(store.values.isEmpty()) }
        assertTrue(RemoteSafetyRepository.validDecision("reason", "response"))
        assertFalse(RemoteSafetyRepository.validDecision("reason", "a".repeat(501)))
    }
}
