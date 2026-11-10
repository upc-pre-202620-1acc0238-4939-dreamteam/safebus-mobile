package com.dreamteam.safebus.iam.data

import com.dreamteam.safebus.iam.domain.Role
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SessionDtoTest {
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    @Test fun mapsStrictRolesAndUsesEarliestExpiration() {
        val dto = SessionDto("token", "Bearer", "DRIVER", now.plusSeconds(600).toString())
        val session = dto.toDomain("8", "DRIVER", 3, now.plusSeconds(300), now)
        assertEquals(Role.DRIVER, session.role)
        assertEquals(now.plusSeconds(300), session.expiresAt)
        assertEquals(session, SessionRecordCodec.decode(SessionRecordCodec.encode(session)))
        assertFalse(dto.toString().contains("token"))
    }
    @Test fun rejectsExpiredMismatchedOrUnknownRolesInsteadOfPersistingThem() {
        var persisted: ByteArray? = null
        for (role in listOf("ADMIN", "SUPERVISOR", "DRIVER")) {
            assertThrows(IllegalArgumentException::class.java) {
                val dto = SessionDto("token", "Bearer", role, now.toString())
                persisted = SessionRecordCodec.encode(dto.toDomain("8", "DRIVER", 3, now, now))
            }
            assertNull(persisted)
        }
        assertThrows(Exception::class.java) { SessionRecordCodec.decode(byteArrayOf(1, 2)) }
        assertNull(persisted)
    }
}
