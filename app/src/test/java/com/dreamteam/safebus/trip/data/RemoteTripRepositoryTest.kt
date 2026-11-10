package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.iam.data.MemoryStore
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class RemoteTripRepositoryTest {
    private val shift = ActiveShift(5, 6, 7, Instant.parse("2026-11-10T12:00:00Z"))
    private val store = MemoryStore()
    private var activated: DomainResult<ActiveShift> = DomainResult.Success(shift)
    private var closed: DomainResult<Unit> = DomainResult.Success(Unit)
    private val service = object : TripService {
        override suspend fun assignment(): DomainResult<Assignment> = DomainResult.Error(Failure.fromStatus(404))
        override suspend fun activate(id: Long, credential: String) = activated
        override suspend fun close(id: Long) = closed
        override suspend fun locate(shift: ActiveShift, position: Position) = DomainResult.Success(Delivery.PENDING)
    }
    private fun repo(owner: String = "user-1") = RemoteTripRepository(service, store, owner, Dispatchers.Unconfined)
    @Test fun preservesConfirmedShiftOnFailedCloseAndScopesRestoration() = runBlocking {
        repo().activate(6, "qr")
        assertEquals(shift, (repo().restore() as DomainResult.Success).value)
        assertNull((repo("user-2").restore() as DomainResult.Success).value)
        closed = DomainResult.Error(Failure(FailureKind.NETWORK))
        assertEquals(closed, repo().close(shift))
        assertEquals(shift, (repo().restore() as DomainResult.Success).value)
        closed = DomainResult.Success(Unit)
        repo().close(shift)
        assertTrue(store.values.isEmpty())
    }
    @Test fun invalidAndRejectedActivationNeverPersistAShift() = runBlocking {
        assertTrue(repo().activate(0, "") is DomainResult.Error)
        assertTrue(store.values.isEmpty())
        for (status in listOf(401, 403, 409, 422)) {
            activated = DomainResult.Error(Failure.fromStatus(status))
            assertEquals(activated, repo().activate(6, "invalid"))
            assertNull((repo().restore() as DomainResult.Success).value)
            assertTrue(store.values.isEmpty())
        }
    }
    @Test fun mapsOnlyActiveResponsesAndRejectsCorruptedRecords() {
        assertEquals(shift, ShiftDto(5, 6, 7, "ACTIVE", shift.startedAt.toString()).toDomain())
        assertThrows(IllegalArgumentException::class.java) { ShiftDto(5, 6, 7, "CLOSED", shift.startedAt.toString()).toDomain() }
        assertEquals(shift, ShiftCodec.decode(ShiftCodec.encode(shift)))
        assertThrows(IllegalArgumentException::class.java) { ShiftCodec.decode(ShiftCodec.encode(shift) + byteArrayOf(1)) }
    }
}
