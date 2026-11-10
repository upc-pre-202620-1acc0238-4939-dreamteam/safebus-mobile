package com.dreamteam.safebus.passenger.data

import com.dreamteam.safebus.passenger.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import com.dreamteam.safebus.iam.data.MemoryStore
import java.time.Instant
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RemotePassengerRepositoryTest {
    private val journey = Journey(1, null, "ABC-123", "Company", "VERIFIED", "Route", "A", "B", "Driver", Instant.parse("2026-11-10T12:00:00Z"))
    private val store = MemoryStore()
    private var started: DomainResult<Journey> = DomainResult.Success(journey)
    private var ended: DomainResult<Delivery> = DomainResult.Success(Delivery.PENDING)
    private var registrations = 0
    private val service = object : PassengerService {
        override suspend fun register(input: Registration): DomainResult<Long> { registrations++; return DomainResult.Success(2L) }
        override suspend fun start(qr: BusQr) = started
        override suspend fun end(journey: Journey, reason: EndReason, separation: Separation?) = ended
        override suspend fun location(busId: Long): DomainResult<Position> = DomainResult.Error(Failure.fromStatus(403))
    }
    private fun repo(owner: String = "passenger") = RemotePassengerRepository(service, store, owner, Dispatchers.Unconfined)
    @Test fun onlyAcknowledgedJourneysAreStoredAndDurableEndDetachesLocally() = runBlocking {
        started = DomainResult.Error(Failure.fromStatus(422))
        assertEquals(started, repo().start(BusQr("invalid")))
        assertNull((repo().restore() as DomainResult.Success).value); assertTrue(store.values.isEmpty())
        started = DomainResult.Success(journey)
        repo().start(BusQr("valid"))
        assertEquals(journey, (repo().restore() as DomainResult.Success).value)
        assertNull((repo("other").restore() as DomainResult.Success).value)
        ended = DomainResult.Error(Failure(FailureKind.STORAGE))
        assertEquals(ended, repo().end(journey, EndReason.MANUAL))
        assertEquals(journey, (repo().restore() as DomainResult.Success).value)
        ended = DomainResult.Success(Delivery.PENDING)
        assertEquals(ended, repo().end(journey, EndReason.MANUAL))
        assertTrue(store.values.isEmpty())
    }
    @Test fun invalidRegistrationNeverReachesNetworkOrPersistsData() = runBlocking {
        val input = Registration("123", "password", true, Photo(byteArrayOf(1), "image/png"))
        assertTrue(repo().register(input) is DomainResult.Error)
        assertEquals(0, registrations); assertTrue(store.values.isEmpty())
        assertEquals(2L, (repo().register(input.copy(dni = "12345678")) as DomainResult.Success).value)
    }
    @Test fun codecAndMapperPreservePrivateFieldsWithoutInventingBusId() {
        assertEquals(journey, JourneyCodec.decode(JourneyCodec.encode(journey)))
        val dto = JourneyDto(1, "ACTIVE", "ABC-123", "Company", "VERIFIED", "Route", "A", "B", "Driver", journey.startedAt.toString())
        assertEquals(journey, dto.toDomain(null))
        assertEquals(9L, dto.toDomain(9).busId)
        assertThrows(IllegalArgumentException::class.java) { dto.copy(status = "ENDED").toDomain(null) }
    }
}
